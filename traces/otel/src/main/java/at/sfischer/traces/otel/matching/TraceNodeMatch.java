package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.TraceNode;

public interface TraceNodeMatch<T extends TraceNode<T>> extends Match<T> {

    default AndSpanMatch<T> and(TraceNodeMatch<T> match){
        //noinspection unchecked
        return new AndSpanMatch<>(this, match);
    }
}
