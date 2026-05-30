package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.Attributes;
import at.sfischer.traces.otel.Span;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributesSpanComparatorTest {

    @Test
    void comparator_sameAttributes() {
        Span span1 = createSpan(
                Map.of("userId", "123",
                        "country", "AT"));
        Span span2 = createSpan(
                Map.of("userId", "123",
                        "country", "AT"));

        AttributesSpanComparator<Span> comparator = new AttributesSpanComparator<>();

        assertTrue(comparator.isSame(span1, span2));
    }

    @Test
    void comparator_differentAttributes() {
        Span span1 = createSpan(Map.of("userId", "123"));
        Span span2 = createSpan(Map.of("userId", "456"));

        AttributesSpanComparator<Span> comparator = new AttributesSpanComparator<>();

        assertFalse(comparator.isSame(span1, span2));
    }

    private Span createSpan(Map<String, String> attributes) {
        Attributes attrs = new Attributes();
        attributes.forEach(attrs::put);

        String name = "test";
        String spanId = "2";
        String traceId = "1";
        String parentSpanId = "1";
        String kind = "KIND";
        String tracer = "TRACER";
        long start = 0;
        long end = 1;

        Span s = new Span(name, spanId, traceId, parentSpanId, kind, tracer, start, end);
        s.putAttributes(attrs);

        return s;
    }
}
