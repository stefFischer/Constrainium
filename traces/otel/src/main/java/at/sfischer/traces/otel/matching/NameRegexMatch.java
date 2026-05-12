package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.TraceNode;

public class NameRegexMatch<T extends TraceNode<T>> implements TraceNodeMatch<T> {

    private final String nameRegex;

    public NameRegexMatch(String nameRegex) {
        this.nameRegex = nameRegex;
    }

    @Override
    public MatchResult matches(T span) {
        return new StringRegexMatch(nameRegex).matches(span.getName());
    }
}
