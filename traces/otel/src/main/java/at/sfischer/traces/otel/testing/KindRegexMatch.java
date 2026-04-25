package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.Span;

public class KindRegexMatch implements SpanMatch {

    private final String kindRegex;

    public KindRegexMatch(String kindRegex) {
        this.kindRegex = kindRegex;
    }

    @Override
    public MatchResult matches(Span span) {
        return new StringRegexMatch(kindRegex).matches(span.getKind());
    }
}
