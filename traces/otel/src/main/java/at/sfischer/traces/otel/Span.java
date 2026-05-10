package at.sfischer.traces.otel;

import java.util.LinkedList;
import java.util.List;

public class Span extends TraceNode<Span> {

    private final String spanId;
    private final String traceId;

    private final String parentSpanId;

    private final String tracer;

    private final long start;
    private final long end;

    // events
    private final List<Event> events;

    // links

    public Span(String name, String spanId, String traceId, String parentSpanId, String kind, String tracer, long start, long end) {
        super(name, kind);
        this.spanId = spanId;
        this.traceId = traceId;
        this.parentSpanId = parentSpanId;
        this.tracer = tracer;
        this.start = start;
        this.end = end;

        this.events = new LinkedList<>();
    }

    public void addEvent(Event event){
        this.events.add(event);
    }

    public void addEvents(List<Event> events){
        this.events.addAll(events);
    }

    public List<Event> getEvents() {
        return events;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public String getTracer() {
        return tracer;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(start + "");
        sb.append(" ");
        sb.append(spanId);
        sb.append(": ");
        sb.append(kind);
        sb.append(" (");
        sb.append(name);
        sb.append(")");
        sb.append(" [");
        sb.append(traceId);
        sb.append("]");
        sb.append(" {");
        sb.append(tracer);
        sb.append("}");
        sb.append(": ");
        boolean first = true;
        for (Attribute<?> attribute : this.attributes.getAttributes()) {
            if(!first){
                sb.append("; ");
            }
            sb.append(attribute);
            first = false;
        }
        sb.append(" {");
        first = true;
        for (Event event : this.events) {
            if(!first){
                sb.append("; ");
            }
            sb.append(event);
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
