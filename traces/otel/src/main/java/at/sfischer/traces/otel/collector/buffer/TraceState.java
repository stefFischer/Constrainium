package at.sfischer.traces.otel.collector.buffer;

import at.sfischer.traces.otel.Span;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TraceState {

    private final String traceId;

    private final List<Span> spans = new ArrayList<>();
    private List<Span> roots = new ArrayList<>();
    private final Map<String, Span> orphans = new LinkedHashMap<>();

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
        this.spans.add(span);
        this.inactiveCycles = 0;
    }

    public List<Span> getSpans() {
        return spans;
    }

    public void notTouched() {
        this.inactiveCycles++;
    }

    public void clearOrphans() {
        this.orphans.clear();
    }

    public Map<String, Span> getOrphans() {
        return orphans;
    }

    public List<Span> getRoots() {
        return roots;
    }

    protected void setRoots(List<Span> roots) {
        this.roots = roots;
    }
}
