package at.sfischer.traces.otel.collector.buffer;

import at.sfischer.traces.otel.Span;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TraceState {

    private final String traceId;

    private List<Span> roots = new ArrayList<>();

    private final Map<String, Span> spansById = new LinkedHashMap<>();
    private final Map<String, List<Span>> waitingChildren = new LinkedHashMap<>();

    private int inactiveCycles;

    public TraceState(String traceId) {
        this.traceId = traceId;
        this.inactiveCycles = 0;
    }

    public String getTraceId() {
        return traceId;
    }

    public int getInactiveCycles() {
        return inactiveCycles;
    }

    public void addSpan(Span span) {
        this.spansById.put(span.getSpanId(), span);
        this.inactiveCycles = 0;
    }

    public boolean containsSpan(Span span) {
        return this.spansById.containsKey(span.getSpanId());
    }

    public Map<String, List<Span>> getWaitingChildren() {
        return waitingChildren;
    }

    public Span getSpan(String spanId){
        return this.spansById.get(spanId);
    }

    public void notTouched() {
        this.inactiveCycles++;
    }

    public List<Span> getRoots() {
        return roots;
    }
}
