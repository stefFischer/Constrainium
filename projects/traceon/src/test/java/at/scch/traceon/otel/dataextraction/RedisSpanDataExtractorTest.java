package at.scch.traceon.otel.dataextraction;

import at.scch.traceon.otel.RedisPayloadDecoder;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.dataextraction.DataExtractor;
import at.sfischer.traces.otel.dataextraction.SpanData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RedisSpanDataExtractorTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void matchesDirectionOperationAndMicrosecondTimeWindow() throws Exception {
        SpanData data = extract(span("HGET"),
                capture("REQUEST", "HGET", command("hget", "cart:123", "SKU-1"), 100, "conn-1"),
                capture("RESPONSE", "HGET", "$1\r\n2\r\n", 900, "conn-1"),
                capture("REQUEST", "HGET", command("hget", "wrong", "field"), 99, "conn-1"),
                capture("RESPONSE", "HGET", "+wrong\r\n", 901, "conn-1"),
                capture("REQUEST", "GET", command("get", "wrong"), 500, "conn-1"));
        JsonNode input = tree(data.getInputData()).path("commands");
        JsonNode output = tree(data.getOutputData()).path("commands");
        assertEquals(1, input.size());
        assertEquals("cart:123", input.get(0).path("key").asText());
        assertEquals("SKU-1", input.get(0).path("field").asText());
        assertEquals(1, output.size());
        assertEquals("2", output.get(0).path("result").asText());
    }

    @Test
    void selectsAndOrdersTransactionCapturesWithoutMixingConnections() throws Exception {
        Span span = span("PIPELINE");
        span.getAttributes().put("db.operation.batch.size", 3L);
        SpanData data = extract(span,
                capture("REQUEST", "EXEC", command("exec"), 500, "conn-1"),
                capture("RESPONSE", "EXEC", "*1\r\n:1\r\n", 600, "conn-1"),
                capture("REQUEST", "MULTI", command("multi"), 200, "conn-1"),
                capture("RESPONSE", "MULTI", "+OK\r\n", 250, "conn-1"),
                capture("REQUEST", "HINCRBY", command("hincrby", "cart:123", "SKU-1", "1"), 300, "conn-1"),
                capture("RESPONSE", "HINCRBY", "+QUEUED\r\n", 350, "conn-1"),
                capture("REQUEST", "GET", command("get", "unrelated"), 400, "conn-2"),
                capture("RESPONSE", "GET", "+unrelated\r\n", 450, "conn-2"));
        JsonNode input = tree(data.getInputData()), output = tree(data.getOutputData());
        assertFalse(input.has("results"));
        assertFalse(output.has("results"));
        assertEquals(3, input.path("commands").size());
        assertEquals("MULTI", input.path("commands").get(0).path("command").asText());
        assertEquals("HINCRBY", input.path("commands").get(1).path("command").asText());
        assertEquals("EXEC", input.path("commands").get(2).path("command").asText());
        assertEquals(1, output.path("commands").get(1).path("result").asInt());
    }

    @Test
    void returnsNullForMissingOrRedactedPayloads() {
        SpanData empty = extract(span("GET"));
        assertNull(empty.getInputData());
        assertNull(empty.getOutputData());
        SpanData redacted = extract(span("GET"),
                capture("REQUEST", "GET", null, 200, "conn-1"),
                capture("RESPONSE", "GET", null, 300, "conn-1"));
        assertNull(redacted.getInputData());
        assertNull(redacted.getOutputData());
    }

    @Test
    void retainsDecodeErrorsAndUnpairedResponses() throws Exception {
        SpanData data = extract(span("GET"),
                capture("REQUEST", "GET", "invalid RESP", 200, "conn-1"),
                capture("RESPONSE", "GET", "+value\r\n", 300, "conn-1"));
        assertTrue(tree(data.getInputData()).path("payload_decode_error").isTextual());
        assertEquals("value", tree(data.getOutputData()).path("payload").asText());
        assertFalse(tree(data.getOutputData()).has("commands"));
    }

    private Span span(String operation) {
        Span span = new Span("redis " + operation, "span", "trace", null, "client", "test",
                1_000_100L, 1_000_900L);
        span.getAttributes().put("db.operation.name", operation);
        return span;
    }

    private SpanData extract(Span span, DataObject... entries) {
        SimpleDataCollection captures = new SimpleDataCollection();
        for (DataObject entry : entries) captures.addDataEntry(entry);
        // Exercise the interface entry point too, as used by generic extraction pipelines.
        DataExtractor extractor = new RedisSpanDataExtractor(captures, new RedisPayloadDecoder());
        return extractor.extractData(span);
    }

    private DataObject capture(String direction, String operation, String payload, int micros, String connection) {
        DataObject capture = new DataObject();
        capture.putValue("source", "redis");
        capture.putValue("direction", direction);
        capture.putValue("@timestamp", String.format(java.util.Locale.ROOT, "1970-01-01T00:00:01.%06dZ", micros));
        capture.putValue("trace_id", connection);
        capture.putValue("attributes.envoy.connection_id", connection);
        DataObject header = new DataObject();
        header.putValue("name", "redis.command"); header.putValue("value", operation);
        capture.putValue("headers", new DataObject[]{header});
        if (payload != null) capture.putValue("payload_inline", Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8)));
        return capture;
    }

    private String command(String... values) {
        StringBuilder resp = new StringBuilder("*" + values.length + "\r\n");
        for (String value : values) resp.append('$').append(value.getBytes(StandardCharsets.UTF_8).length)
                .append("\r\n").append(value).append("\r\n");
        return resp.toString();
    }

    private JsonNode tree(DataObject data) throws Exception {
        assertNotNull(data);
        return json.readTree(data.toJson());
    }
}
