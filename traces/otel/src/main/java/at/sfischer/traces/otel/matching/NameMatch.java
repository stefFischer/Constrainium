package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.TraceNode;

public class NameMatch<T extends TraceNode<T>> implements TraceNodeMatch<T> {

    private final String name;

    public NameMatch(String name) {
        this.name = name;
    }

    @Override
    public MatchResult matches(T span) {
        return new StringEqualsMatch(name).matches(span.getName());
    }
}
