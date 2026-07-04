package at.sfischer.traces.otel.abstraction;

import at.sfischer.traces.otel.Attributes;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.matching.MatchResult;
import at.sfischer.traces.otel.matching.TraceNodeMatch;

public abstract class SpanAbstractor {

    private final TraceNodeMatch<Span> match;

    public SpanAbstractor(TraceNodeMatch<Span> match) {
        this.match = match;
    }

    public boolean matches(Span span){
        return match.matches(span) == MatchResult.SUCCESS;
    }

    public static void transferAttributes(Span span, AbstractSpan abstractSpan, String... attributeNames){
        Attributes toTransfer = span.getAttributes().getSubSet(attributeNames);
        abstractSpan.putAttributes(toTransfer);
    }

    public abstract AbstractSpan abstractSpan(Span span);
}
