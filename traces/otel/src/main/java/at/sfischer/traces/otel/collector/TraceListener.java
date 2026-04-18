package at.sfischer.traces.otel.collector;

import at.sfischer.traces.otel.Span;

import java.util.List;

public interface TraceListener {
    void spansCollected(List<Span> spans);
}
