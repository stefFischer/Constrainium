package at.sfischer.traces.otel.filter;

import at.sfischer.traces.otel.TraceNode;
import at.sfischer.traces.otel.matching.MatchResult;
import at.sfischer.traces.otel.matching.TraceNodeMatch;

public class MatchFilter<T extends TraceNode<T>> implements TraceFilter<T> {

    private final TraceNodeMatch<T> match;

    public MatchFilter(TraceNodeMatch<T> match) {
        this.match = match;
    }

    @Override
    public boolean include(T node) {
        return match.matches(node) == MatchResult.SUCCESS;
    }
}
