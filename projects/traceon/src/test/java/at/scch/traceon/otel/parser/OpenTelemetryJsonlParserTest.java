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

    private Span parseOne(String json) {
        List<Span> spans = new ArrayList<>();
        TraceListener<Span> listener = spans::addAll;
        parser.parse(new StringReader(json), listener);
        assertEquals(1, spans.size());
        return spans.getFirst();
    }
}
