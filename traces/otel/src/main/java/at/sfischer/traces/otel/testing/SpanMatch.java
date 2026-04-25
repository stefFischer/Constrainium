package at.sfischer.traces.otel.testing;


import at.sfischer.traces.otel.Span;

import java.util.stream.Stream;

public interface SpanMatch extends Match<Span> {

    static NameMatch name(String name){
        return new NameMatch(name);
    }

    static NameRegexMatch nameRegex(String regex){
        return new NameRegexMatch(regex);
    }

    static KindMatch kind(String kind){
        return new KindMatch(kind);
    }

    static KindRegexMatch kindRegex(String regex){
        return new KindRegexMatch(regex);
    }

    static TracerMatch tracer(String tracer){
        return new TracerMatch(tracer);
    }

    static AttributeExistsMatch attributeExists(String attributeName){
        return new AttributeExistsMatch(attributeName);
    }

    static AttributeRegexMatch attributeRegex(String attributeName, String valueRegex){
        return new AttributeRegexMatch(attributeName, valueRegex);
    }

    static <T> AttributeMatch<T> attribute(String attributeName, T value){
        return new AttributeMatch<>(attributeName, value);
    }

    static AttributeJsonMatch attributeJson(String attributeName, JSONMatch... jsonMatches){
        return new AttributeJsonMatch(attributeName, jsonMatches);
    }

    static <T> JSONFieldValueMatch<T> json(T value, JsonPathStepMatch<?>... steps){
        return new JSONFieldValueMatch<>(JsonPathStepMatch.jsonPath(steps), value);
    }

    static JSONMultipleMatch json(JsonPathStepMatch<?> pathSteps, JSONMatch... matches){
        return new JSONMultipleMatch(pathSteps, matches);
    }

    static AndSpanMatch and(SpanMatch match1, SpanMatch match2, SpanMatch... matches){
        SpanMatch[] allMatches = Stream.concat(Stream.of(match1, match2), Stream.of(matches)).toArray(SpanMatch[]::new);
        return new AndSpanMatch(allMatches);
    }

    default AndSpanMatch and(SpanMatch match){
        return new AndSpanMatch(this, match);
    }

}
