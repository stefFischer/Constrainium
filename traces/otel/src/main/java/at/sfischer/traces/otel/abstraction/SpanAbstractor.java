package at.sfischer.traces.otel.abstraction;

import at.sfischer.traces.otel.Span;

public interface SpanAbstractor {

    AbstractSpan abstractSpan(Span span);
}
