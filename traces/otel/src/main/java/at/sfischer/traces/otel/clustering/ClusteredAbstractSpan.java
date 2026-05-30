package at.sfischer.traces.otel.clustering;

import at.sfischer.traces.otel.Attribute;
import at.sfischer.traces.otel.TraceNode;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.dataextraction.SpanData;

import java.util.LinkedList;
import java.util.List;

public class ClusteredAbstractSpan extends TraceNode<ClusteredAbstractSpan> {

    private final List<SpanData> spanData;

    public ClusteredAbstractSpan(AbstractSpan baseSpan) {
        super(baseSpan.getName(), baseSpan.getKind());
        this.spanData = new LinkedList<>();
        this.spanData.add(baseSpan.getSpanData());
        putAttributes(baseSpan.getAttributes().clone());

        for (AbstractSpan child : baseSpan.getChildren()) {
            this.addChild(new ClusteredAbstractSpan(child));
        }
    }

    public void addSpan(AbstractSpan baseSpan){
        if(baseSpan.getSpanData() != null){
            this.spanData.add(baseSpan.getSpanData());
        }
        if(this.getChildren().size() != baseSpan.getChildren().size()){
            throw new IllegalArgumentException("Span must have the same children to be matched.");
        }
        for (int i = 0; i < this.getChildren().size(); i++) {
            ClusteredAbstractSpan child = this.getChildren().get(i);
            AbstractSpan baseChild = baseSpan.getChildren().get(i);
            child.addSpan(baseChild);
        }
    }

    public List<SpanData> getSpanData() {
        return spanData;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(kind);
        sb.append(" (");
        sb.append(name);
        sb.append(")");
        sb.append(": ");
        boolean first = true;
        for (Attribute<?> attribute : this.attributes.getAttributes()) {
            if(!first){
                sb.append("; ");
            }
            sb.append(attribute);
            first = false;
        }

        return sb.toString();
    }
}
