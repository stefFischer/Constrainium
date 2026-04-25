package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.Span;

public class AttributeJsonMatch implements SpanMatch {

    private final String key;

    private final JSONMatch[] jsonMatches;

    public AttributeJsonMatch(String key, JSONMatch... jsonMatches) {
        this.key = key;
        this.jsonMatches = jsonMatches;
    }

    @Override
    public MatchResult matches(Span span) {
        String value = span.getAttribute(key);
        if(value == null){
            return new FailExpectedValueResult(key, null);
        }

        return new AndMatch<>(jsonMatches).matches(value);
    }
}
