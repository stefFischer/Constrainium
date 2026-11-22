package at.scch.traceon.otel.dataextraction;

import at.scch.traceon.otel.PayloadDecoder;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataValue;
import at.sfischer.traces.otel.dataextraction.SpanData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Adds protocol-level argument roles without inferring application-specific field names. */
final class RedisCommandDataExtractor {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final PayloadDecoder decoder;

    RedisCommandDataExtractor(PayloadDecoder decoder) {
        this.decoder = decoder;
    }

    SpanData enrich(DataCollection<DataObject> requests, DataCollection<DataObject> responses, DataObject input, DataObject output) {
        List<DataObject> requestList = ordered(requests);
        List<DataObject> responseList = ordered(responses);
        List<ObjectNode> commands = new ArrayList<>();
        for (DataObject request : requestList) {
            commands.add(describe(payload(request)));
        }

        List<JsonNode> replies = new ArrayList<>();
        Map<DataObject, JsonNode> normalizedReplies = new IdentityHashMap<>();
        for (DataObject response : responseList) {
            JsonNode reply = payload(response);
            JsonNode normalized = normalizeReply(header(response, "redis.command"), reply);
            replies.add(normalized);
            if (normalized != reply) {
                normalizedReplies.put(response, normalized);
            }
        }

        List<ObjectNode> results = new ArrayList<>();
        // Missing/extra captures must not shift the positional correspondence.
        if (aligned(requestList, responseList)) {
            List<Integer> transaction = null;
            for (int i = 0; i < commands.size(); i++) {
                ObjectNode command = commands.get(i);
                JsonNode reply = replies.get(i);
                results.add(command == null ? null : command.deepCopy());
                if (command == null || reply == null) {
                    transaction = null;
                    continue;
                }

                String name = command.path("command").asText();
                if ("MULTI".equals(name)) {
                    transaction = reply.isTextual() && "OK".equals(reply.asText()) ? new ArrayList<>() : null;
                } else if ("EXEC".equals(name)) {
                    if (transaction != null && reply.isArray() && reply.size() == transaction.size()) {
                        ArrayNode normalized = reply.deepCopy();
                        boolean changed = false;
                        for (int j = 0; j < transaction.size(); j++) {
                            ObjectNode queued = results.get(transaction.get(j));
                            JsonNode result = normalizeReply(queued.path("command").asText(), reply.get(j));
                            normalized.set(j, result);
                            changed |= result != reply.get(j);
                            addResult(queued, result);
                        }
                        if (changed) {
                            reply = normalized;
                            normalizedReplies.put(responseList.get(i), normalized);
                        }
                    }
                    transaction = null;
                } else if ("DISCARD".equals(name)) {
                    transaction = null;
                } else if (reply.isTextual() && "QUEUED".equals(reply.asText())) {
                    if (transaction != null) transaction.add(i);
                    results.get(i).put("acknowledgement", "QUEUED");
                    continue;
                } else if (transaction != null) {
                    // A failed or incomplete transaction cannot be mapped safely.
                    transaction = null;
                }
                addResult(results.get(i), reply);
            }
        }

        input = append(input, commands);
        output = normalizeOutput(output, responses, normalizedReplies);
        output = append(output, results);
        return new SpanData(input, output);
    }

    private static DataObject append(DataObject data, List<ObjectNode> commands) {
        List<DataObject> entries = new ArrayList<>();
        for (ObjectNode command : commands) {
            if (command != null){
                entries.add(toDataObject(command));
            }
        }

        if (entries.isEmpty()){
            return data;
        }

        // The legacy multi-capture wrapper is redundant only when every capture
        // has a usable command representation. Keep it for partial extraction.
        boolean complete = entries.size() == commands.size()
                && commands.stream().allMatch(command -> !command.has("acknowledgement")
                        || command.has("result") || command.has("result_is_null"));
        if (complete && data != null && data.getDataValue("results") != null) {
            data = withoutResults(data);
        }
        if (data == null){
            data = new DataObject();
        }

        data.putValue("commands", entries.toArray(DataObject[]::new));
        return data;
    }

    private static DataObject withoutResults(DataObject data) {
        try {
            ObjectNode object = (ObjectNode) JSON.readTree(data.toJson());
            object.remove("results");
            return toDataObject(object);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not remove legacy Redis results", e);
        }
    }

    private static JsonNode normalizeReply(String command, JsonNode reply) {
        if (!"HGETALL".equalsIgnoreCase(command) || reply == null) {
            return reply;
        }

        ArrayNode entries = JSON.createArrayNode();
        if (reply.isObject()) {
            reply.fields().forEachRemaining(field -> {
                ObjectNode entry = entries.addObject();
                entry.put("field", field.getKey());
                entry.set("value", field.getValue());
            });
        } else if (reply.isArray() && reply.size() % 2 == 0) {
            for (int index = 0; index < reply.size(); index += 2) {
                ObjectNode entry = entries.addObject();
                entry.set("field", reply.get(index));
                entry.set("value", reply.get(index + 1));
            }
        } else {
            return reply;
        }

        return JSON.createObjectNode().set("entries", entries);
    }

    private static DataObject normalizeOutput(DataObject output, DataCollection<DataObject> responses, Map<DataObject, JsonNode> normalizedReplies) {
        if (output == null || normalizedReplies.isEmpty()) {
            return output;
        }

        try {
            ObjectNode normalized = (ObjectNode) JSON.readTree(output.toJson());
            List<DataObject> decodedCaptures = new ArrayList<>();
            responses.visitDataObjects((capture, entry) -> {
                DataValue<?> encoded = capture.getDataValue("payload_inline");
                if (encoded != null && encoded.getValue() instanceof String) {
                    decodedCaptures.add(capture);
                }
            });
            // The fallback payloads retain capture order, whereas command pairing uses timestamp order.
            for (int index = 0; index < decodedCaptures.size(); index++) {
                JsonNode replacement = normalizedReplies.get(decodedCaptures.get(index));
                if (replacement != null) {
                    ObjectNode record = decodedCaptures.size() == 1 ? normalized
                            : (ObjectNode) normalized.path("results").get(index);
                    record.set("payload", replacement);
                }
            }

            return toDataObject(normalized);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not normalize Redis hash results", exception);
        }
    }

    private static DataObject toDataObject(ObjectNode object) {
        DataObject data = DataObject.parseData(object.toString());
        // The JSON parser omits empty arrays; normalized entries still have a known object-array type.
        preserveEmptyEntries(data, object);
        return data;
    }

    private static void preserveEmptyEntries(DataObject data, ObjectNode object) {
        object.fields().forEachRemaining(field -> {
            JsonNode value = field.getValue();
            if (field.getKey().equals("entries") && value.isArray() && value.isEmpty()) {
                data.putValue("entries", new DataObject[0]);
                return;
            }

            DataValue<?> stored = data.getDataValue(field.getKey());
            if (stored != null && stored.getValue() instanceof DataObject nested && value instanceof ObjectNode node) {
                preserveEmptyEntries(nested, node);
            } else if (stored != null && stored.getValue() instanceof DataObject[] elements && value.isArray()) {
                for (int index = 0; index < elements.length; index++) {
                    if (value.get(index) instanceof ObjectNode node) {
                        preserveEmptyEntries(elements[index], node);
                    }
                }
            }
        });
    }

    private JsonNode payload(DataObject capture) {
        DataValue<?> value = capture.getDataValue("payload_inline");
        if (value == null || !(value.getValue() instanceof String encoded)) {
            return null;
        }

        try {
            JsonNode decoded = JSON.readTree(decoder.decode(encoded).toJson());
            // DataObject omits empty arrays; an empty HGETALL array is an empty hash, not RESP null.
            if (!decoded.has("payload") && "HGETALL".equalsIgnoreCase(header(capture, "redis.command"))
                    && new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8).equals("*0\r\n")) {
                return JSON.createArrayNode();
            }

            // DataObject omits JSON null fields, although RESP null is a valid reply.
            return decoded.has("payload") ?
                    decoded.get("payload")
                    : com.fasterxml.jackson.databind.node.NullNode.getInstance();
        } catch (Exception ignored) {
            // The original extraction retains decoding errors; do not invent a command.
            return null;
        }
    }

    private static ObjectNode describe(JsonNode payload) {
        if (payload == null || !payload.isArray() || payload.isEmpty() || !payload.get(0).isTextual()){
            return null;
        }

        ObjectNode command = JSON.createObjectNode();
        String name = payload.get(0).asText().toUpperCase(Locale.ROOT);
        command.put("command", name);
        command.set("arguments", JSON.createArrayNode().addAll(java.util.stream.IntStream.range(1, payload.size()).mapToObj(payload::get).toList()));
        switch (name) {
            case "GET", "HGETALL", "TTL", "PTTL", "INCR", "DECR", "PERSIST", "TYPE" -> role(command, payload, 1, "key");
            case "HGET", "HEXISTS" -> {
                role(command, payload, 1, "key"); role(command, payload, 2, "field");
            }
            case "HINCRBY", "HINCRBYFLOAT" -> {
                role(command, payload, 1, "key"); role(command, payload, 2, "field");
                role(command, payload, 3, "increment");
            }
            case "SET", "SETNX", "GETSET" -> {
                role(command, payload, 1, "key"); role(command, payload, 2, "value");
            }
            case "EXPIRE", "PEXPIRE", "EXPIREAT", "PEXPIREAT" -> {
                role(command, payload, 1, "key");
                String field = switch (name) {
                    case "EXPIRE" -> "timeout_seconds";
                    case "PEXPIRE" -> "timeout_milliseconds";
                    case "EXPIREAT" -> "unix_time_seconds";
                    default -> "unix_time_milliseconds";
                };
                role(command, payload, 2, field);
            }
            case "DEL", "UNLINK", "EXISTS", "MGET" -> command.set("keys", command.get("arguments"));
            case "HDEL", "HMGET" -> {
                role(command, payload, 1, "key");
                command.set("fields", JSON.createArrayNode().addAll(
                        java.util.stream.IntStream.range(2, payload.size()).mapToObj(payload::get).toList()));
            }
            case "HSET", "HMSET" -> {
                role(command, payload, 1, "key");
                ArrayNode fields = command.putArray("entries");
                for (int i = 2; i + 1 < payload.size(); i += 2) {
                    ObjectNode entry = fields.addObject();
                    entry.set("field", payload.get(i)); entry.set("value", payload.get(i + 1));
                }
            }
            default -> { /* Unknown commands retain all positional arguments. */ }
        }

        return command;
    }

    private static void role(ObjectNode command, JsonNode payload, int index, String name) {
        if (index >= payload.size()){
            return;
        }

        JsonNode value = payload.get(index);
        if (name.equals("increment") || name.startsWith("timeout_") || name.startsWith("unix_time_")) {
            try {
                command.put(name, new java.math.BigDecimal(value.asText()));
                return;
            } catch (NumberFormatException ignored) { /* Retain malformed arguments verbatim. */ }
        }

        command.set(name, value);
    }

    private static void addResult(ObjectNode command, JsonNode reply) {
        if (reply.isNull()){
            command.put("result_is_null", true);
        } else {
            command.set("result", reply);
        }

        if ("HGETALL".equals(command.path("command").asText()) && reply.path("entries").isArray()) {
            command.set("entries", reply.get("entries"));
        }
    }

    private static List<DataObject> ordered(DataCollection<DataObject> captures) {
        List<DataObject> result = new ArrayList<>();
        captures.visitDataObjects((capture, entry) -> result.add(capture));
        result.sort(Comparator.comparing(capture -> text(capture, "@timestamp")));
        return result;
    }

    private static boolean aligned(List<DataObject> requests, List<DataObject> responses) {
        if (requests.size() != responses.size()) {
            return false;
        }

        for (int i = 0; i < requests.size(); i++) {
            DataObject request = requests.get(i), response = responses.get(i);
            if (connection(request).isEmpty() || header(request, "redis.command").isEmpty()
                    || !Objects.equals(connection(request), connection(response))
                    || !header(request, "redis.command").equalsIgnoreCase(header(response, "redis.command"))){
                return false;
            }
        }

        return true;
    }

    private static String connection(DataObject capture) {
        DataValue<?> attributes = capture.getDataValue("attributes");
        if (attributes != null && attributes.getValue() instanceof DataObject object) {
            String id = text(object, "envoy.connection_id");
            if (!id.isEmpty()) {
                return id;
            }
        }

        return text(capture, "trace_id");
    }

    private static String header(DataObject capture, String name) {
        DataValue<?> headers = capture.getDataValue("headers");
        if (headers != null && headers.getValue() instanceof DataObject[] entries) {
            for (DataObject entry : entries){
                if (name.equals(text(entry, "name"))){
                    return text(entry, "value");
                }
            }
        }

        return "";
    }

    private static String text(DataObject data, String name) {
        DataValue<?> value = data.getDataValue(name);
        return value == null ? "" : String.valueOf(value.getValue());
    }
}
