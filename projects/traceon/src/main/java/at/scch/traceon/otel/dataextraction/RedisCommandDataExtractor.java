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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

        List<ObjectNode> results = new ArrayList<>();
        // Missing/extra captures must not shift the positional correspondence.
        if (aligned(requestList, responseList)) {
            List<Integer> transaction = null;
            for (int i = 0; i < commands.size(); i++) {
                ObjectNode command = commands.get(i);
                JsonNode reply = payload(responseList.get(i));
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
                        for (int j = 0; j < transaction.size(); j++) {
                            addResult(results.get(transaction.get(j)), reply.get(j));
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
        output = append(output, results);
        return new SpanData(input, output);
    }

    private static DataObject append(DataObject data, List<ObjectNode> commands) {
        List<DataObject> entries = new ArrayList<>();
        for (ObjectNode command : commands) {
            if (command != null){
                entries.add(DataObject.parseData(command.toString()));
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
            return DataObject.parseData(object.toString());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not remove legacy Redis results", e);
        }
    }

    private JsonNode payload(DataObject capture) {
        DataValue<?> value = capture.getDataValue("payload_inline");
        if (value == null || !(value.getValue() instanceof String encoded)) {
            return null;
        }

        try {
            JsonNode decoded = JSON.readTree(decoder.decode(encoded).toJson());
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
                var fields = command.putArray("entries");
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

        if ("HGETALL".equals(command.path("command").asText())) {
            ArrayNode entries = JSON.createArrayNode();
            if (reply.isArray() && reply.size() % 2 == 0) {
                for (int i = 0; i < reply.size(); i += 2) {
                    ObjectNode entry = entries.addObject();
                    entry.set("field", reply.get(i)); entry.set("value", reply.get(i + 1));
                }
                command.set("entries", entries);
            } else if (reply.isObject()) {
                reply.fields().forEachRemaining(field -> {
                    ObjectNode entry = entries.addObject();
                    entry.put("field", field.getKey()); entry.set("value", field.getValue());
                });
                command.set("entries", entries);
            }
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
