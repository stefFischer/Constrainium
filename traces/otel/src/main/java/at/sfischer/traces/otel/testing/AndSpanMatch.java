package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.Span;

public class AndSpanMatch extends AndMatch<Span> implements SpanMatch{
    public AndSpanMatch(SpanMatch... matches) {
        super(matches);
    }
}
