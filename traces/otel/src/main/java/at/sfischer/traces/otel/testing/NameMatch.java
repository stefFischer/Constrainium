package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.Span;

public class NameMatch implements SpanMatch {

    private final String name;

    public NameMatch(String name) {
        this.name = name;
    }

    @Override
    public MatchResult matches(Span span) {
        return new StringEqualsMatch(name).matches(span.getName());
    }
}
