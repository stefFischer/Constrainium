package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.Span;

public class KindMatch implements SpanMatch {

    private final String kind;

    public KindMatch(String kind) {
        this.kind = kind;
    }

    @Override
    public MatchResult matches(Span span) {
        return new StringEqualsMatch(kind).matches(span.getKind());
    }
}
