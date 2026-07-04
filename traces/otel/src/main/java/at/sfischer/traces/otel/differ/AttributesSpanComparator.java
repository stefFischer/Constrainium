package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.Attributes;
import at.sfischer.traces.otel.TraceNode;

import java.util.Objects;
import java.util.Set;

public class AttributesSpanComparator<T extends TraceNode<T>> implements SpanComparator<T> {

    @Override
    public boolean isSame(T a, T b) {
        Attributes aa = a.getAttributes();
        Attributes ab = b.getAttributes();
        return Objects.equals(
                Set.copyOf(aa.getAttributes()),
                Set.copyOf(ab.getAttributes()));
    }
}
