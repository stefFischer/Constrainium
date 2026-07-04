package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.TraceNode;

import java.util.Set;

public class AndSpanComparator<T extends TraceNode<T>> implements SpanComparator<T> {

    private final Set<SpanComparator<T>> comparators;

    public AndSpanComparator(Set<SpanComparator<T>> comparators) {
        this.comparators = comparators;
    }

    public AndSpanComparator(SpanComparator<T>... comparators) {
        this(Set.of(comparators));
    }

    @Override
    public boolean isSame(T a, T b) {
        for (SpanComparator<T> comparator : this.comparators) {
            if(!comparator.isSame(a,b)){
                return false;
            }
        }

        return true;
    }
}
