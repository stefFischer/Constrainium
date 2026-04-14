package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.Span;

public class NameOnlySpanComparator implements SpanComparator {
    @Override
    public boolean isSame(Span a, Span b) {
        return a.getName().equals(b.getName());
    }
}
