package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.Span;

public class TracerRegexMatch implements SpanMatch {

    private final String tracerRegex;

    public TracerRegexMatch(String tracerRegex) {
        this.tracerRegex = tracerRegex;
    }

    @Override
    public MatchResult matches(Span span) {
        return new StringRegexMatch(tracerRegex).matches(span.getTracer());
    }
}
