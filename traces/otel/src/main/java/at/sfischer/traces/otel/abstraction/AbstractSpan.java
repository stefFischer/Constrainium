package at.sfischer.traces.otel.abstraction;

import at.sfischer.traces.otel.Attribute;
import at.sfischer.traces.otel.TraceNode;

public class AbstractSpan extends TraceNode<AbstractSpan> {

    public AbstractSpan(String name, String kind) {
        super(name, kind);
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
