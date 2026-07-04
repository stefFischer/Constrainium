package at.sfischer.traces.otel.collector;

import at.sfischer.traces.otel.TraceNode;

import java.util.List;

public interface TraceListener<T extends TraceNode<T>> {
    void spansCollected(List<T> spans);
}
