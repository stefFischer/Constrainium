package at.scch.traceon.otel.parser;

import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.collector.TraceListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenTelemetryJsonlParserTest {
    private final OpenTelemetryJsonlParser parser = new OpenTelemetryJsonlParser();

    @Test
    void parsesMultipleJsonlRecordsWithUtf8AndMicrosecondPrecision() {
        String jsonl = """
                {"operationName":"GET /grün", "traceID":"trace", "spanID":"root",
                 "startTime":1794222692715097, "startTimeMillis":1794222692715, "duration":11355,
                 "references":[], "tag":{"span@kind":"server"}}

                {"operationName":"redis pipeline", "traceID":"trace", "spanID":"child",
                 "startTime":1794222692715101, "duration":42,
                 "references":[{"refType":"FOLLOWS_FROM","spanID":"unrelated"},
                               {"refType":"CHILD_OF","spanID":"root"}],
                 "tag":{"span@kind":"client"}}
                """;
        List<List<Span>> batches = new ArrayList<>();
        TraceListener<Span> listener = batches::add;
        parser.parse(new ByteArrayInputStream(jsonl.getBytes(StandardCharsets.UTF_8)), listener);

        assertEquals(1, batches.size());
        List<Span> spans = batches.getFirst();
        assertEquals(2, spans.size());
        Span root = spans.get(0), child = spans.get(1);
        assertEquals("GET /grün", root.getName());
        assertEquals("trace", root.getTraceId());
        assertEquals("root", root.getSpanId());
        assertEquals("server", root.getKind());
        assertNull(root.getParentSpanId());
        assertEquals(1794222692715097L, root.getStart());
        assertEquals(1794222692726452L, root.getEnd());
        assertEquals("child", child.getSpanId());
        assertEquals("root", child.getParentSpanId());
        assertEquals(1794222692715143L, child.getEnd());
    }

    @Test
    void normalizesAttributesAndPreservesTypesAndResourceInformation() throws Exception {
        Span span = parseOne("""
                {"operationName":"redis pipeline", "traceID":"trace", "spanID":"span",
                 "tag":{"span@kind":"client", "otel@scope@name":"cart-service/redis",
                        "db@system":"redis", "db@operation@batch@size":4,
                        "large@number":4294967296, "ratio":1.25, "enabled":true,
                        "optional":null, "nested":{"id":"SKU-1"}, "items":["a","b"]},
                 "process":{"serviceName":"cart-service",
                            "tag":{"service@version":"0.1.0", "service@instance@id":"instance-1"}}}
                """);

        assertEquals("client", span.getKind());
        assertEquals("cart-service/redis", span.getTracer());
        assertNull(span.getAttribute("span.kind"));
        assertNull(span.getAttribute("otel.scope.name"));
        assertEquals("redis", span.getAttribute("db.system"));
        assertEquals(Long.valueOf(4), span.<Object>getAttribute("db.operation.batch.size"));
        assertEquals(Long.valueOf(4294967296L), span.<Object>getAttribute("large.number"));
        assertEquals(Double.valueOf(1.25), span.<Object>getAttribute("ratio"));
        assertEquals(Boolean.TRUE, span.getAttribute("enabled"));
        assertNull(span.getAttribute("optional"));
        assertEquals("cart-service", span.getAttribute("service.name"));
        assertEquals("0.1.0", span.getAttribute("service.version"));
        assertEquals("instance-1", span.getAttribute("service.instance.id"));
        ObjectMapper json = new ObjectMapper();
        assertEquals(json.readTree("{\"id\":\"SKU-1\"}"),
                json.readTree(span.<String>getAttribute("nested")));
        assertEquals(json.readTree("[\"a\",\"b\"]"),
                json.readTree(span.<String>getAttribute("items")));
    }

    @Test
    void acceptsMissingOptionalMetadataWithoutInventingAParent() {
        Span span = parseOne("""
                {"operationName":"work", "traceID":"trace", "spanID":"span",
                 "startTime":100, "duration":5,
                 "references":[{"refType":"FOLLOWS_FROM","spanID":"other"}]}
                """);
        assertNull(span.getParentSpanId());
        assertNull(span.getKind());
        assertNull(span.getTracer());
        assertTrue(span.getAttributes().getAttributes().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "42", "{\"operationName\":"})
    void rejectsInvalidRecordsWithoutDeliveringAPartialTrace(String invalidRecord) {
        List<List<Span>> batches = new ArrayList<>();
        TraceListener<Span> listener = batches::add;
        String validRecord = "{\"operationName\":\"work\",\"traceID\":\"trace\",\"spanID\":\"span\"}";
        assertThrows(RuntimeException.class,
                () -> parser.parse(new StringReader(validRecord + "\n" + invalidRecord), listener));
        assertTrue(batches.isEmpty(), "A malformed file must not publish its valid prefix as a complete batch");
    }

    @Test
    void parsesPersistenceLogsAsSeparateEventsWithNanosecondTimestamps() {
        Span span = parseOne("""
                {"operationName":"kafka.consume","spanID":"span","startTime":1794222715392984,
                 "logs":[
                   {"timestamp":1794222715409033,"fields":[
                     {"key":"event","type":"string","value":"order persisted"},
                     {"key":"order_id","type":"string","value":"01a1c001-f201-7720-a7e5-9b1f6c3b4f88"},
                     {"key":"cart_id","type":"string","value":"f56ac64e-2146-4166-9894-66d6fcf544fc"},
                     {"key":"level","type":"string","value":"INFO"},
                     {"key":"code@lineno","type":"int64","value":"90"}]},
                   {"timestamp":1794222715410000,"fields":[
                     {"key":"event","type":"string","value":"order published"},
                     {"key":"order_id","type":"string","value":"another-order"}]}]}
                """);
        assertEquals(2, span.getEvents().size());
        var persisted = span.getEvents().get(0);
        assertEquals("order persisted", persisted.getName());
        assertEquals(1794222715409033000L, persisted.getEpochNanos());
        assertEquals("order persisted", persisted.getAttribute("event"));
        assertEquals("01a1c001-f201-7720-a7e5-9b1f6c3b4f88", persisted.getAttribute("order_id"));
        assertEquals("f56ac64e-2146-4166-9894-66d6fcf544fc", persisted.getAttribute("cart_id"));
        assertEquals("INFO", persisted.getAttribute("level"));
        assertEquals(Long.valueOf(90), persisted.<Object>getAttribute("code.lineno"));
        assertEquals("order published", span.getEvents().get(1).getName());
        assertEquals("another-order", span.getEvents().get(1).getAttribute("order_id"));
        assertNull(span.getAttribute("order_id"));
        assertEquals(1794222715392984L, span.getStart());
    }

    @Test
    void decodesDeclaredLogTypesAndPreservesUnknownOrInvalidValues() {
        Span span = parseOne("""
                {"logs":[{"timestamp":100,"fields":[
                  {"key":"count","type":"int64","value":4294967296},
                  {"key":"ratio","type":"float64","value":"1.25"},
                  {"key":"enabled","type":"bool","value":"true"},
                  {"key":"disabled","type":"bool","value":false},
                  {"key":"opaque","type":"binary","value":"AQID"},
                  {"key":"badNumber","type":"int64","value":"not-a-number"},
                  {"key":"badBoolean","type":"bool","value":"unknown"},
                  {"key":"optional","type":"string","value":null}]}]}
                """);
        var event = span.getEvents().getFirst();
        assertEquals("log", event.getName());
        assertEquals(Long.valueOf(4294967296L), event.<Object>getAttribute("count"));
        assertEquals(Double.valueOf(1.25), event.<Object>getAttribute("ratio"));
        assertEquals(Boolean.TRUE, event.getAttribute("enabled"));
        assertEquals(Boolean.FALSE, event.getAttribute("disabled"));
        assertEquals("AQID", event.getAttribute("opaque"));
        assertEquals("not-a-number", event.getAttribute("badNumber"));
        assertEquals("unknown", event.getAttribute("badBoolean"));
        assertNull(event.getAttribute("optional"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"logs\":[]}", "{\"logs\":null}"})
    void acceptsSpansWithoutLogs(String record) {
        assertTrue(parseOne(record).getEvents().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"timestamp\":\"invalid\"}", "{\"timestamp\":9223372036854775807}"})
    void rejectsMissingInvalidOrOverflowingLogTimestamps(String log) {
        assertThrows(IllegalArgumentException.class, () -> parseOne("{\"logs\":[" + log + "]}"));
    }

    private Span parseOne(String json) {
        List<Span> spans = new ArrayList<>();
        TraceListener<Span> listener = spans::addAll;
        parser.parse(new StringReader(json), listener);
        assertEquals(1, spans.size());
        return spans.getFirst();
    }
}
