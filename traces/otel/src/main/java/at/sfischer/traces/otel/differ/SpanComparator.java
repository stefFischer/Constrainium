package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.Span;

public interface SpanComparator {
    boolean isSame(Span a, Span b);
}
