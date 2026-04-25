package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.Span;

public class AttributeMatch<T> implements SpanMatch {

    private final String key;

    private final T value;

    public AttributeMatch(String key, T value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public MatchResult matches(Span span) {
        T value = span.getAttribute(key);
        return new ObjectEqualsMatch<>(this.value).matches(value);
    }
}
