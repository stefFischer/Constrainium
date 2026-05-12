package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.TraceNode;

public class AttributeMatch<ATT, T extends TraceNode<T>> implements TraceNodeMatch<T> {

    private final String key;

    private final ATT value;

    public AttributeMatch(String key, ATT value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public MatchResult matches(T span) {
        ATT value = span.getAttribute(key);
        return new ObjectEqualsMatch<>(this.value).matches(value);
    }
}
