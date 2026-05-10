package at.sfischer.traces.otel.filter;

import at.sfischer.traces.otel.TraceNode;

public class MinSpanCountFilter<T extends TraceNode<T>> implements TraceFilter<T> {

    private final int minCount;

    public MinSpanCountFilter(int minCount) {
        this.minCount = minCount;
    }

    @Override
    public boolean include(T node) {
        return node.countSpans() >= minCount;
    }
}
