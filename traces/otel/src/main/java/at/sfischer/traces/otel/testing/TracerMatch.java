package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.Span;

public class TracerMatch implements SpanMatch {

    private final String tracer;

    public TracerMatch(String tracer) {
        this.tracer = tracer;
    }

    @Override
    public MatchResult matches(Span span) {
        return new StringEqualsMatch(tracer).matches(span.getTracer());
    }
}
