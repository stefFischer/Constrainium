package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.TraceNode;

public interface SpanComparator<T extends TraceNode<T>> {
    boolean isSame(T a, T b);
}
