package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.Span;

public class AttributeRegexMatch implements SpanMatch {

    private final String key;

    private final String valueRegex;

    public AttributeRegexMatch(String key, String valueRegex) {
        this.key = key;
        this.valueRegex = valueRegex;
    }

    @Override
    public MatchResult matches(Span span) {
        String value = span.getAttribute(key);
        return new StringRegexMatch(valueRegex).matches(value);
    }
}
