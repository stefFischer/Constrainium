package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.TraceNode;

public class AndSpanMatch<T extends TraceNode<T>> extends AndMatch<T> implements TraceNodeMatch<T> {
    public AndSpanMatch(TraceNodeMatch<T>... matches) {
        super(matches);
    }
}
