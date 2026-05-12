package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.TraceNode;

public class AttributeExistsMatch<T extends TraceNode<T>> implements TraceNodeMatch<T> {

    private final String key;

    public AttributeExistsMatch(String key) {
        this.key = key;
    }

    @Override
    public MatchResult matches(T span) {
        Object value = span.getAttribute(key);
        if(value != null){
            return MatchResult.SUCCESS;
        }

        return new FailResult("Expected Attribute \"" + this.key + "\" does not exist.");
    }
}
