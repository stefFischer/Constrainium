package at.sfischer.traces.otel.dataextraction;

import at.sfischer.traces.otel.TraceNode;

import java.util.function.Function;

@FunctionalInterface
public interface SpanAttributeExtractor<T> extends Function<TraceNode<?>, T> {
    T extract(TraceNode<?> span);

    @Override
    default T apply(TraceNode<?> span) {
        return extract(span);
    }

    static SpanAttributeExtractor<String> attribute(String key) {
        return span -> span.getAttribute(key);
    }

    static SpanAttributeExtractor<String> attributeOrElse(String key, String fallback) {
        return span -> {
            String value = span.getAttribute(key);
            return value != null ? value : fallback;
        };
    }

    static SpanAttributeExtractor<String> firstAttribute(String... keys) {
        return span -> {
            for (String key : keys) {
                String value = span.getAttribute(key);
                if (value != null) return value;
            }
            return null;
        };
    }
}
