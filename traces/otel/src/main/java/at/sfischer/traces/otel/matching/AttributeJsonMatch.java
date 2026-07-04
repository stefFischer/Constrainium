package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.TraceNode;
import at.sfischer.traces.otel.testing.FailExpectedValueResult;

public class AttributeJsonMatch<T extends TraceNode<T>> implements TraceNodeMatch<T> {

    private final String key;

    private final JSONMatch[] jsonMatches;

    public AttributeJsonMatch(String key, JSONMatch... jsonMatches) {
        this.key = key;
        this.jsonMatches = jsonMatches;
    }

    @Override
    public MatchResult matches(T span) {
        String value = span.getAttribute(key);
        if(value == null){
            return new FailExpectedValueResult(key, null);
        }

        return new AndMatch<>(jsonMatches).matches(value);
    }
}
