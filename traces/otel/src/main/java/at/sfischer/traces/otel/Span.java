package at.sfischer.traces.otel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Span {

    private final String name;
    private final String spanId;
    private final String traceId;

    private final String parentSpanId;

    private final String kind;
    private final String tracer;

    private final long start;
    private final long end;

    // attributes
    private final Attributes attributes;

    // events
    private final List<Event> events;

    // links

    // children
    public final List<Span> children;

    public Span(String name, String spanId, String traceId, String parentSpanId, String kind, String tracer, long start, long end) {
        this.name = name;
        this.spanId = spanId;
        this.traceId = traceId;
        this.parentSpanId = parentSpanId;
        this.kind = kind;
        this.tracer = tracer;
        this.start = start;
        this.end = end;

        this.attributes = new Attributes();
        this.events = new LinkedList<>();

        this.children = new LinkedList<>();
    }

    public void putAttributes(Attributes attributes){
        this.attributes.putAll(attributes);
    }

    public <T> T getAttribute(String key){
        return this.attributes.get(key);
    }

    public <T> T removeAttribute(String key){
        return this.attributes.remove(key);
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

    public String getName() {
        return name;
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

    public String getKind() {
        return kind;
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

    public List<Span> getChildren() {
        return children;
    }

    public void visitSpans(SpanVisitor visitor){
        boolean visitChildren = visitor.visitSpan(this);
        if(visitChildren) {
            this.children.forEach(child -> child.visitSpans(visitor));
        }
    }

    public int countSpans(){
        final int[] count = {0};
        this.visitSpans(span -> {
            count[0]++;
            return true;
        });

        return count[0];
    }

    public int getDepth() {
        if (children.isEmpty()) {
            return 1;
        }

        int maxChildDepth = 0;
        for (Span child : children) {
            int childDepth = child.getDepth();
            if (childDepth > maxChildDepth) {
                maxChildDepth = childDepth;
            }
        }

        return 1 + maxChildDepth;
    }

    public int getBreadth() {
        Map<Integer, Integer> levelCounts = new HashMap<>();
        computeBreadth(0, levelCounts);

        int maxBreadth = 0;
        for (int count : levelCounts.values()) {
            if (count > maxBreadth) {
                maxBreadth = count;
            }
        }

        return maxBreadth;
    }

    private void computeBreadth(int level, Map<Integer, Integer> levelCounts) {
        levelCounts.put(level, levelCounts.getOrDefault(level, 0) + 1);
        for (Span child : children) {
            child.computeBreadth(level + 1, levelCounts);
        }
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

    public String toStringWithChildren() {
        return toStringWithChildren("");
    }

    protected String toStringWithChildren(String indent){
        StringBuilder sb = new StringBuilder(indent);
        sb.append(this);
        for (Span child : this.children) {
            sb.append("\n");
            sb.append(child.toStringWithChildren(indent + "\t"));
        }
        return sb.toString();
    }
}
