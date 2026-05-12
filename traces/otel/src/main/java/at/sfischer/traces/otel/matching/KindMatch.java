package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.TraceNode;

public class KindMatch<T extends TraceNode<T>> implements TraceNodeMatch<T> {

    private final String kind;

    public KindMatch(String kind) {
        this.kind = kind;
    }

    @Override
    public MatchResult matches(T span) {
        return new StringEqualsMatch(kind).matches(span.getKind());
    }
}
