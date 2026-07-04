package at.sfischer.traces.otel.collector;

import at.sfischer.traces.otel.TraceNode;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RecordingTraceListener<T extends TraceNode<T>> implements TraceListener<T> {

    private final Queue<T> spans = new ConcurrentLinkedQueue<>();

    @Override
    public void spansCollected(List<T> spans) {
        this.spans.addAll(spans);
    }

    /**
     * Returns all collected spans and clears the internal buffer.
     */
    public List<T> drain() {
        List<T> drained = new ArrayList<>();
        T span;
        while ((span = spans.poll()) != null) {
            drained.add(span);
        }
        return drained;
    }

    /**
     * Returns a snapshot without clearing.
     */
    public List<T> getAll() {
        return new ArrayList<>(spans);
    }

    public boolean isEmpty() {
        return spans.isEmpty();
    }
}
