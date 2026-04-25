package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.Span;

public class AttributeExistsMatch implements SpanMatch {

    private final String key;

    public AttributeExistsMatch(String key) {
        this.key = key;
    }

    @Override
    public MatchResult matches(Span span) {
        Object value = span.getAttribute(key);
        if(value != null){
            return MatchResult.SUCCESS;
        }

        return new FailResult("Expected Attribute \"" + this.key + "\" does not exist.");
    }
}
