package at.sfischer.traces.otel.collector;

import at.sfischer.traces.otel.Span;

import java.util.LinkedList;
import java.util.List;

public abstract class TraceCollector {

    private final List<TraceListener<Span>> listeners = new LinkedList<>();

    public void addTraceListener(TraceListener<Span> listener){
        this.listeners.add(listener);
    }

    public void removeTraceListener(TraceListener<Span> listener){
        this.listeners.remove(listener);
    }

    protected void fireSpansCollected(List<Span> spans){
        for (TraceListener<Span> listener : this.listeners) {
            listener.spansCollected(spans);
        }
    }

    public abstract void collect();
}
