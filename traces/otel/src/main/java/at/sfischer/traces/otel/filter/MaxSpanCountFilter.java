package at.sfischer.traces.otel.filter;

import at.sfischer.traces.otel.TraceNode;

public class MaxSpanCountFilter<T extends TraceNode<T>> implements TraceFilter<T> {

    private final int maxCount;

    public MaxSpanCountFilter(int maxCount) {
        this.maxCount = maxCount;
    }

    @Override
    public boolean include(T node) {
        return node.countSpans() <= maxCount;
    }
}
