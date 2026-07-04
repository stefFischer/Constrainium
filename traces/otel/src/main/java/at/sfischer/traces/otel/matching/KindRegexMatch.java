package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.TraceNode;

public class KindRegexMatch<T extends TraceNode<T>> implements TraceNodeMatch<T> {

    private final String kindRegex;

    public KindRegexMatch(String kindRegex) {
        this.kindRegex = kindRegex;
    }

    @Override
    public MatchResult matches(T span) {
        return new StringRegexMatch(kindRegex).matches(span.getKind());
    }
}
