package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.TraceNode;

public class NameOnlySpanComparator<T extends TraceNode<T>> implements SpanComparator<T> {
    @Override
    public boolean isSame(T a, T b) {
        return a.getName().equals(b.getName());
    }
}
