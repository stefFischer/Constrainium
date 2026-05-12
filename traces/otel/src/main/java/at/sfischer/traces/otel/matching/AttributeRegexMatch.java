package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.TraceNode;

public class AttributeRegexMatch<T extends TraceNode<T>> implements TraceNodeMatch<T> {

    private final String key;

    private final String valueRegex;

    public AttributeRegexMatch(String key, String valueRegex) {
        this.key = key;
        this.valueRegex = valueRegex;
    }

    @Override
    public MatchResult matches(T span) {
        String value = span.getAttribute(key);
        return new StringRegexMatch(valueRegex).matches(value);
    }
}
