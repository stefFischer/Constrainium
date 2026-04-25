package at.sfischer.traces.otel.collector;

import at.sfischer.traces.otel.Span;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RecordingTraceListener implements TraceListener {

    private final Queue<Span> spans = new ConcurrentLinkedQueue<>();

    @Override
    public void spansCollected(List<Span> spans) {
        this.spans.addAll(spans);
    }

    /**
     * Returns all collected spans and clears the internal buffer.
     */
    public List<Span> drain() {
        List<Span> drained = new ArrayList<>();
        Span span;
        while ((span = spans.poll()) != null) {
            drained.add(span);
        }
        return drained;
    }

    /**
     * Returns a snapshot without clearing.
     */
    public List<Span> getAll() {
        return new ArrayList<>(spans);
    }

    public boolean isEmpty() {
        return spans.isEmpty();
    }
}
