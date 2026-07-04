package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.TraceNode;

import java.util.Objects;

public class AttributeSpanComparator<T extends TraceNode<T>> implements SpanComparator<T> {

    private final String attributeKey;

    public AttributeSpanComparator(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    @Override
    public boolean isSame(T a, T b) {
        Object aa = a.getAttribute(this.attributeKey);
        Object ab = b.getAttribute(this.attributeKey);
        return Objects.equals(aa, ab);
    }
}
