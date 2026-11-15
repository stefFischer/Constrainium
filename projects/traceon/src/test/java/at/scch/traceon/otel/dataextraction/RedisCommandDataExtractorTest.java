package at.scch.traceon.otel.dataextraction;

import at.scch.traceon.otel.RedisPayloadDecoder;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.traces.otel.dataextraction.SpanData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RedisCommandDataExtractorTest {
    private final RedisCommandDataExtractor extractor = new RedisCommandDataExtractor(new RedisPayloadDecoder());
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void mapsExecResultsToQueuedCommands() throws Exception {
        var requests = new SimpleDataCollection();
        requests.addDataEntry(capture("MULTI", command("multi"), 1));
        requests.addDataEntry(capture("HINCRBY", command("hincrby", "cart:123", "SKU-1", "1"), 2));
        requests.addDataEntry(capture("EXPIRE", command("expire", "cart:123", "300"), 3));
        requests.addDataEntry(capture("EXEC", command("exec"), 4));
        var responses = new SimpleDataCollection();
        responses.addDataEntry(capture("MULTI", "+OK\r\n", 5));
        responses.addDataEntry(capture("HINCRBY", "+QUEUED\r\n", 6));
        responses.addDataEntry(capture("EXPIRE", "+QUEUED\r\n", 7));
        responses.addDataEntry(capture("EXEC", "*2\r\n:1\r\n:1\r\n", 8));
        DataObject legacy = DataObject.parseData("{results:[{payload:'legacy'}]}");
        SpanData data = extractor.enrich(requests, responses, legacy.clone(), legacy.clone());
        assertNull(data.getInputData().getDataValue("results"));
        assertNull(data.getOutputData().getDataValue("results"));
        JsonNode input = json.readTree(data.getInputData().toJson()).path("commands");
        JsonNode output = json.readTree(data.getOutputData().toJson()).path("commands");
        assertEquals("cart:123", input.get(1).path("key").asText());
        assertEquals("SKU-1", input.get(1).path("field").asText());
        assertEquals(1, input.get(1).path("increment").asInt());
        assertEquals(300, input.get(2).path("timeout_seconds").asInt());
        assertEquals(1, output.get(1).path("result").asInt());
        assertEquals(1, output.get(2).path("result").asInt());
        assertEquals("QUEUED", output.get(1).path("acknowledgement").asText());
    }

    @Test
    void retainsLegacyResultsWhenCommandExtractionIsIncomplete() {
        var requests = new SimpleDataCollection();
        requests.addDataEntry(capture("GET", command("get", "key"), 1));
        requests.addDataEntry(capture("GET", "not RESP", 2));
        DataObject legacy = DataObject.parseData("{results:[{payload:'value'},{payload_decode_error:'invalid'}]}");
        SpanData data = extractor.enrich(requests, new SimpleDataCollection(), legacy, legacy.clone());
        assertNotNull(data.getInputData().getDataValue("results"));
        assertNotNull(data.getInputData().getDataValue("commands"));
        assertNotNull(data.getOutputData().getDataValue("results"));
    }

    @Test
    void labelsHashResultsAndPreservesUnknownArguments() throws Exception {
        var requests = new SimpleDataCollection();
        requests.addDataEntry(capture("HGETALL", command("hgetall", "cart:123"), 1));
        requests.addDataEntry(capture("CUSTOM", command("custom", "opaque", "argument"), 2));
        var responses = new SimpleDataCollection();
        responses.addDataEntry(capture("HGETALL", command("SKU-1", "2"), 3));
        responses.addDataEntry(capture("CUSTOM", "+OK\r\n", 4));
        SpanData data = extractor.enrich(requests, responses, null, null);
        JsonNode output = json.readTree(data.getOutputData().toJson()).path("commands");
        assertEquals("SKU-1", output.get(0).path("entries").get(0).path("field").asText());
        assertEquals("2", output.get(0).path("entries").get(0).path("value").asText());
        assertEquals("opaque", output.get(1).path("arguments").get(0).asText());
        assertFalse(output.get(1).has("key"));
    }

    @Test
    void doesNotPairMissingOrDifferentConnectionResponses() {
        var requests = new SimpleDataCollection();
        requests.addDataEntry(capture("GET", command("get", "key"), 1));
        assertNull(extractor.enrich(requests, new SimpleDataCollection(), null, null).getOutputData());
        var responses = new SimpleDataCollection();
        DataObject response = capture("GET", "+value\r\n", 2);
        response.putValue("trace_id", "different-connection");
        responses.addDataEntry(response);
        assertNull(extractor.enrich(requests, responses, null, null).getOutputData());
    }

    @Test
    void abortedExecDoesNotAssignResultsToQueuedCommands() throws Exception {
        var requests = new SimpleDataCollection();
        requests.addDataEntry(capture("MULTI", command("multi"), 1));
        requests.addDataEntry(capture("HGET", command("hget", "cart:123", "SKU-1"), 2));
        requests.addDataEntry(capture("EXEC", command("exec"), 3));
        var responses = new SimpleDataCollection();
        responses.addDataEntry(capture("MULTI", "+OK\r\n", 4));
        responses.addDataEntry(capture("HGET", "+QUEUED\r\n", 5));
        responses.addDataEntry(capture("EXEC", "*-1\r\n", 6));
        SpanData data = extractor.enrich(requests, responses, null, null);
        JsonNode output = json.readTree(data.getOutputData().toJson()).path("commands");
        assertFalse(output.get(1).has("result"));
        assertTrue(output.get(2).path("result_is_null").asBoolean());
    }

    @Test
    void integratesWithRedisSpanExtraction() throws Exception {
        var captures = new SimpleDataCollection();
        DataObject request = capture("HGET", command("hget", "cart:123", "SKU-1"), 1);
        request.putValue("direction", "REQUEST");
        DataObject response = capture("HGET", "$1\r\n2\r\n", 2);
        response.putValue("direction", "RESPONSE");
        captures.addDataEntry(request); captures.addDataEntry(response);
        var span = new at.sfischer.traces.otel.Span("redis hget", "span", "trace", null,
                "client", "test", 0, Long.MAX_VALUE / 2);
        span.getAttributes().put("db.operation.name", "HGET");
        SpanData data = new RedisSpanDataExtractor(captures, new RedisPayloadDecoder()).extractData(span);
        JsonNode output = json.readTree(data.getOutputData().toJson());
        assertEquals("2", output.path("payload").asText());
        assertEquals("HGET", output.path("commands").get(0).path("command").asText());
        assertEquals("2", output.path("commands").get(0).path("result").asText());
    }

    @Test
    void preservesNullResultsAndRawPayloads() throws Exception {
        var requests = new SimpleDataCollection();
        requests.addDataEntry(capture("GET", command("get", "key"), 1));
        var responses = new SimpleDataCollection();
        responses.addDataEntry(capture("GET", "$-1\r\n", 2));
        DataObject raw = new DataObject(); raw.putValue("payload", "original");
        SpanData data = extractor.enrich(requests, responses, raw, null);
        assertEquals("original", data.getInputData().getDataValue("payload").getValue());
        assertTrue(json.readTree(data.getOutputData().toJson()).path("commands").get(0).path("result_is_null").asBoolean());
    }

    private DataObject capture(String name, String resp, int time) {
        DataObject capture = new DataObject();
        capture.putValue("@timestamp", String.format("2026-11-09T11:11:%02dZ", time));
        capture.putValue("trace_id", "conn-1");
        capture.putValue("payload_inline", Base64.getEncoder().encodeToString(resp.getBytes(StandardCharsets.UTF_8)));
        DataObject header = new DataObject(); header.putValue("name", "redis.command"); header.putValue("value", name);
        capture.putValue("headers", new DataObject[]{header});
        return capture;
    }

    private String command(String... values) {
        StringBuilder result = new StringBuilder("*" + values.length + "\r\n");
        for (String value : values) result.append('$').append(value.getBytes(StandardCharsets.UTF_8).length)
                .append("\r\n").append(value).append("\r\n");
        return result.toString();
    }
}
