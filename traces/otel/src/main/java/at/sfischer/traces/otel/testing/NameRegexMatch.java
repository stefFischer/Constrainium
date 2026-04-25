package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.Span;

public class NameRegexMatch implements SpanMatch {

    private final String nameRegex;

    public NameRegexMatch(String nameRegex) {
        this.nameRegex = nameRegex;
    }

    @Override
    public MatchResult matches(Span span) {
        return new StringRegexMatch(nameRegex).matches(span.getName());
    }
}
