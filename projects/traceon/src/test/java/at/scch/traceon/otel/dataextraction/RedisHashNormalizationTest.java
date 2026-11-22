package at.scch.traceon.otel.dataextraction;

import at.scch.traceon.otel.RedisPayloadDecoder;
import at.scch.traceon.otel.graph.TraceonTypePromotionPolicy;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.data.model.CallRelation;
import at.sfischer.constraints.data.model.DataFlow;
import at.sfischer.constraints.data.model.DatabaseQueryEdge;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.constraints.data.model.GraphNode;
import at.sfischer.constraints.data.model.SynchronousCallEdge;
import at.sfischer.constraints.model.ArrayType;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.dataextraction.SpanData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RedisHashNormalizationTest {
    private static final Set<String> IDS = Set.of("BRAN-001", "PUMP-001", "HAY-002", "STRAW-003", "__owner");
    private final ObjectMapper json = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void normalizesArrayAndMapRepliesWithoutModifyingCaptures(boolean mapReply) throws Exception {
        String[] fields = {"__owner", "owner", "BRAN-001", "5", "PUMP-001", "3"};
        String reply = mapReply ? respMap(fields) : respArray(fields);
        SimpleDataCollection captures = records(new String[][]{{"hgetall", "opaque-key"}}, new String[]{reply});
        List<String> original = captures.getDataCollection().stream().map(DataObject::toJson).toList();

        SpanData data = extract("HGETALL", captures, 1);

        JsonNode output = tree(data.getOutputData());
        Map<String, String> expected = Map.of("__owner", "owner", "BRAN-001", "5", "PUMP-001", "3");
        assertEquals(expected, entries(output.path("payload").path("entries")));
        JsonNode command = output.path("commands").get(0);
        assertEquals(expected, entries(command.path("entries")));
        assertEquals(expected, entries(command.path("result").path("entries")));
        assertEquals("opaque-key", command.path("key").asText());
        assertNoIdentifierProperties(output);
        SimpleDataSchema schema = SimpleDataSchema.deriveFromData(data.getOutputData());
        assertEquals(TypeEnum.STRING, schema.findDataSchemaEntry("payload.entries.field").type);
        assertEquals(TypeEnum.STRING, schema.findDataSchemaEntry("payload.entries.value").type);
        assertNoIdentifierFields(schema);
        assertEquals(original, captures.getDataCollection().stream().map(DataObject::toJson).toList());
        assertEquals("opaque-key", tree(data.getInputData()).path("payload").get(1).asText());
    }

    @Test
    void differentHashFieldsProduceTheSameSchema() {
        SpanData first = single(respMap("BRAN-001", "5", "PUMP-001", "3"));
        SpanData second = single(respMap("HAY-002", "2", "STRAW-003", "1"));

        assertEquals(SimpleDataSchema.deriveFromData(first.getOutputData()),
                SimpleDataSchema.deriveFromData(second.getOutputData()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"*0\r\n", "%0\r\n"})
    void preservesEmptyHashesAsTypedEntriesArrays(String reply) throws Exception {
        SpanData data = single(reply);

        JsonNode output = tree(data.getOutputData());
        assertTrue(output.path("payload").path("entries").isArray());
        assertEquals(0, output.path("payload").path("entries").size());
        assertTrue(output.path("commands").get(0).path("entries").isArray());
        assertTrue(output.path("commands").get(0).path("result").path("entries").isArray());
        SimpleDataSchema schema = SimpleDataSchema.deriveFromData(data.getOutputData());
        assertEquals(new ArrayType(TypeEnum.COMPLEXTYPE), schema.findDataSchemaEntry("payload.entries").type);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void normalizesQueuedHashResultsAndTheirCopiesInTheExecResult(boolean mapReply) throws Exception {
        String[][] commands = {{"multi"}, {"hgetall", "first-key"}, {"hgetall", "second-key"}, {"exec"}};
        String first = mapReply ? respMap("BRAN-001", "5") : respArray("BRAN-001", "5");
        String second = mapReply ? respMap("PUMP-001", "3") : respArray("PUMP-001", "3");
        String[] replies = {"+OK\r\n", "+QUEUED\r\n", "+QUEUED\r\n",
                "*2\r\n" + first + second};
        SimpleDataCollection captures = records(commands, replies);
        List<String> original = captures.getDataCollection().stream().map(DataObject::toJson).toList();

        SpanData data = extract("PIPELINE", captures, commands.length);

        JsonNode output = tree(data.getOutputData());
        assertFalse(output.has("results"));
        JsonNode results = output.path("commands");
        assertEquals("OK", results.get(0).path("result").asText());
        assertEquals(Map.of("BRAN-001", "5"), entries(results.get(1).path("result").path("entries")));
        assertEquals(Map.of("PUMP-001", "3"), entries(results.get(2).path("entries")));
        assertEquals("QUEUED", results.get(1).path("acknowledgement").asText());
        assertEquals(Map.of("BRAN-001", "5"), entries(results.get(3).path("result").get(0).path("entries")));
        assertEquals(Map.of("PUMP-001", "3"), entries(results.get(3).path("result").get(1).path("entries")));
        assertNoIdentifierProperties(output);
        assertNoIdentifierFields(SimpleDataSchema.deriveFromData(data.getOutputData(), new TraceonTypePromotionPolicy()));
        assertEquals(original, captures.getDataCollection().stream().map(DataObject::toJson).toList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void normalizesFallbackPayloadsWhenCommandExtractionIsIncomplete(boolean reverseCaptureOrder) throws Exception {
        SimpleDataCollection captures = records(new String[][]{{"hgetall", "first-key"}, {"hgetall", "second-key"}},
                new String[]{respMap("BRAN-001", "5"), respArray("PUMP-001", "3")});
        captures.getDataCollection().get(2).putValue("payload_inline", encoded("invalid RESP"));
        if (reverseCaptureOrder) {
            Collections.reverse(captures.getDataCollection());
        }
        List<String> original = captures.getDataCollection().stream().map(DataObject::toJson).toList();

        SpanData data = extract("PIPELINE", captures, 2);

        JsonNode output = tree(data.getOutputData());
        assertTrue(output.has("results"));
        assertEquals(Map.of("BRAN-001", "5"), entries(output.path("results").get(0).path("payload").path("entries")));
        assertEquals(Map.of("PUMP-001", "3"), entries(output.path("results").get(1).path("payload").path("entries")));
        assertNoIdentifierProperties(output);
        assertNoIdentifierFields(SimpleDataSchema.deriveFromData(data.getOutputData()));
        assertTrue(tree(data.getInputData()).path("results").get(1).has("payload_decode_error"));
        assertEquals(original, captures.getDataCollection().stream().map(DataObject::toJson).toList());
    }

    @Test
    void normalizesOnlyHashResultsInMixedPipelines() throws Exception {
        SimpleDataCollection captures = records(new String[][]{{"hgetall", "first-key"}, {"exists", "second-key"}},
                new String[]{respMap("BRAN-001", "5", "PUMP-001", "3"), ":1\r\n"});

        SpanData data = extract("PIPELINE", captures, 2);

        JsonNode output = tree(data.getOutputData());
        assertFalse(output.has("results"));
        JsonNode commands = output.path("commands");
        assertEquals(Map.of("BRAN-001", "5", "PUMP-001", "3"), entries(commands.get(0).path("result").path("entries")));
        assertEquals(1, commands.get(1).path("result").asInt());
        assertFalse(commands.get(1).has("entries"));
        assertNoIdentifierProperties(output);
    }

    @Test
    void normalizesAHashResponseEvenWhenItsRequestWasNotCaptured() throws Exception {
        SimpleDataCollection captures = new SimpleDataCollection();
        captures.addDataEntry(capture("RESPONSE", "HGETALL", respMap("BRAN-001", "5"), 200));

        SpanData data = extract("HGETALL", captures, 1);

        JsonNode output = tree(data.getOutputData());
        assertEquals(Map.of("BRAN-001", "5"), entries(output.path("payload").path("entries")));
        assertFalse(output.has("commands"));
        assertNoIdentifierProperties(output);
    }

    @Test
    void leavesOtherCommandMapsAndHashErrorsUnchanged() throws Exception {
        SpanData other = extract("CUSTOM", records(new String[][]{{"custom"}},
                new String[]{respMap("BRAN-001", "5")}), 1);
        assertEquals("5", tree(other.getOutputData()).path("payload").path("BRAN-001").asText());
        assertEquals("5", tree(other.getOutputData()).path("commands").get(0).path("result").path("BRAN-001").asText());
        SpanData error = single("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n");
        JsonNode output = tree(error.getOutputData());
        assertTrue(output.path("payload").asText().startsWith("WRONGTYPE"));
        assertFalse(output.path("commands").get(0).has("entries"));
    }

    @Test
    void infersHashFieldToProductIdFlowsAcrossArrayAndMapReplies() {
        Graph graph = graph(false);

        graph.finalizeGraph();

        SynchronousCallEdge request = (SynchronousCallEdge) graph.getNode("caller").getOutgoing().getFirst();
        DatabaseQueryEdge redis = (DatabaseQueryEdge) graph.getNode("service").getOutgoing().getFirst();
        DataSchemaEntry<SimpleDataSchema> productId = request.getSchema().findDataSchemaEntry("output.payload.items.productId");
        assertEquals(2, redis.getDataFlowsTo(request).size());
        for (String path : List.of("output.payload.entries.field", "output.commands.entries.field",
                "output.commands.result.entries.field")) {
            DataSchemaEntry<SimpleDataSchema> field = redis.getSchema().findDataSchemaEntry(path);
            assertNotNull(field);
            for (DataFlow flow : redis.getDataFlowsTo(request)) {
                assertSame(productId, flow.getDataFlows().get(field));
                assertEquals(0, flow.getSourceRow());
                assertEquals(0, flow.getTargetRow());
            }
        }
        assertTrue(redis.getDataByTrace().isEmpty());
        assertNoIdentifierFields(redis.getSchema().getOutputSchema());
    }

    @Test
    void aMissingProductIsACounterexampleAcrossComparableExecutions() {
        Graph graph = graph(true);

        graph.finalizeGraph();

        SynchronousCallEdge request = (SynchronousCallEdge) graph.getNode("caller").getOutgoing().getFirst();
        DatabaseQueryEdge redis = (DatabaseQueryEdge) graph.getNode("service").getOutgoing().getFirst();
        assertEquals(2, redis.getDataFlowsTo(request).size());
        for (String path : List.of("output.payload.entries.field", "output.commands.entries.field",
                "output.commands.result.entries.field")) {
            DataSchemaEntry<SimpleDataSchema> field = redis.getSchema().findDataSchemaEntry(path);
            assertNotNull(field);
            assertTrue(redis.getDataFlowsTo(request).stream().allMatch(flow -> !flow.getDataFlows().containsKey(field)));
        }
    }

    private Graph graph(boolean counterexample) {
        Graph graph = new Graph();
        GraphNode caller = graph.getOrCreateNode(new GraphNode("caller", "caller"));
        GraphNode service = graph.getOrCreateNode(new GraphNode("service", "service"));
        GraphNode database = graph.getOrCreateNode(new GraphNode("redis", "redis"));
        SynchronousCallEdge request = new SynchronousCallEdge(caller, service);
        DatabaseQueryEdge redis = new DatabaseQueryEdge(service, database, "HGETALL");
        SpanData first = single(respArray("__owner", "owner-a", "BRAN-001", "5", "PUMP-001", "3"));
        SpanData second = single(respMap("__owner", "owner-b", "HAY-002", "2", "STRAW-003", "1"));
        request.addData("first", new DataObject(), DataObject.parseData(
                "{payload:{items:[{productId:'BRAN-001'},{productId:'PUMP-001'}]}}"));
        request.addData("second", new DataObject(), DataObject.parseData(
                "{payload:{items:[{productId:'HAY-002'},{productId:'%s'}]}}".formatted(counterexample ? "MISSING" : "STRAW-003")));
        redis.addData("first", first.getInputData(), first.getOutputData());
        redis.addData("second", second.getInputData(), second.getOutputData());
        graph.addCallRelationGroup(List.of(new CallRelation("first", request, 0, redis, 0),
                new CallRelation("second", request, 0, redis, 0)));
        return graph;
    }

    private SpanData single(String reply) {
        return extract("HGETALL", records(new String[][]{{"hgetall", "opaque-key"}}, new String[]{reply}), 1);
    }

    private SpanData extract(String operation, SimpleDataCollection captures, int batchSize) {
        Span span = new Span("redis " + operation, "span", "trace", null, "client", "test", 1_000_100L, 1_000_900L);
        span.getAttributes().put("db.operation.name", operation);
        span.getAttributes().put("db.operation.batch.size", batchSize);
        return new RedisSpanDataExtractor(captures, new RedisPayloadDecoder()).extractData(span);
    }

    private static SimpleDataCollection records(String[][] commands, String[] replies) {
        SimpleDataCollection captures = new SimpleDataCollection();
        for (int index = 0; index < commands.length; index++) {
            String operation = commands[index][0].toUpperCase(Locale.ROOT);
            captures.addDataEntry(capture("REQUEST", operation, respArray(commands[index]), 100 + index * 100));
            captures.addDataEntry(capture("RESPONSE", operation, replies[index], 150 + index * 100));
        }
        return captures;
    }

    private static DataObject capture(String direction, String operation, String payload, int micros) {
        DataObject capture = new DataObject();
        capture.putValue("source", "redis");
        capture.putValue("direction", direction);
        capture.putValue("@timestamp", String.format(Locale.ROOT, "1970-01-01T00:00:01.%06dZ", micros));
        capture.putValue("trace_id", "connection");
        capture.putValue("attributes.envoy.connection_id", "connection");
        capture.putValue("payload_inline", encoded(payload));
        DataObject header = new DataObject();
        header.putValue("name", "redis.command");
        header.putValue("value", operation);
        capture.putValue("headers", new DataObject[]{header});
        return capture;
    }

    private static String respArray(String... values) {
        return respValues('*', values.length, values);
    }

    private static String respMap(String... values) {
        return respValues('%', values.length / 2, values);
    }

    private static String respValues(char type, int size, String[] values) {
        StringBuilder resp = new StringBuilder().append(type).append(size).append("\r\n");
        for (String value : values) {
            resp.append('$').append(value.getBytes(StandardCharsets.UTF_8).length)
                    .append("\r\n").append(value).append("\r\n");
        }
        return resp.toString();
    }

    private static String encoded(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode tree(DataObject data) throws Exception {
        assertNotNull(data);
        return json.readTree(data.toJson());
    }

    private static Map<String, String> entries(JsonNode entries) {
        assertTrue(entries.isArray());
        Map<String, String> fields = new LinkedHashMap<>();
        for (JsonNode entry : entries) {
            fields.put(entry.path("field").asText(), entry.path("value").asText());
        }
        return fields;
    }

    private static void assertNoIdentifierProperties(JsonNode node) {
        if (node.isObject()) {
            node.fields().forEachRemaining(field -> {
                assertFalse(IDS.contains(field.getKey()));
                assertNoIdentifierProperties(field.getValue());
            });
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                assertNoIdentifierProperties(child);
            }
        }
    }

    private static void assertNoIdentifierFields(SimpleDataSchema schema) {
        for (String name : schema.getSchemaEntryNames()) {
            assertFalse(IDS.contains(name));
            DataSchemaEntry<SimpleDataSchema> entry = schema.getSchemaEntry(name);
            if (entry.dataSchema != null) {
                assertNoIdentifierFields(entry.dataSchema);
            }
        }
    }
}
