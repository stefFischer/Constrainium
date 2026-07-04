package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.abstraction.AbstractSpan;

import java.util.stream.Stream;

public interface AbstractSpanMatch extends TraceNodeMatch<AbstractSpan> {

    static NameMatch<AbstractSpan> name(String name){
        return new NameMatch<>(name);
    }

    static NameRegexMatch<AbstractSpan> nameRegex(String regex){
        return new NameRegexMatch<>(regex);
    }

    static KindMatch<AbstractSpan> kind(String kind){
        return new KindMatch<>(kind);
    }

    static KindRegexMatch<AbstractSpan> kindRegex(String regex){
        return new KindRegexMatch<>(regex);
    }

    static AttributeExistsMatch<AbstractSpan> attributeExists(String attributeName){
        return new AttributeExistsMatch<>(attributeName);
    }

    static AttributeRegexMatch<AbstractSpan> attributeRegex(String attributeName, String valueRegex){
        return new AttributeRegexMatch<>(attributeName, valueRegex);
    }

    static <T> AttributeMatch<T, AbstractSpan> attribute(String attributeName, T value){
        return new AttributeMatch<>(attributeName, value);
    }

    static AttributeJsonMatch<AbstractSpan> attributeJson(String attributeName, JSONMatch... jsonMatches){
        return new AttributeJsonMatch<>(attributeName, jsonMatches);
    }

    static <T> JSONFieldValueMatch<T> json(T value, JsonPathStepMatch<AbstractSpan>... steps){
        return new JSONFieldValueMatch<>(JsonPathStepMatch.jsonPath(steps), value);
    }

    static JSONMultipleMatch json(JsonPathStepMatch<AbstractSpan> pathSteps, JSONMatch... matches){
        return new JSONMultipleMatch(pathSteps, matches);
    }

    static AndSpanMatch<AbstractSpan> and(TraceNodeMatch<AbstractSpan> match1, TraceNodeMatch<AbstractSpan> match2, TraceNodeMatch<AbstractSpan>... matches){
        //noinspection unchecked
        TraceNodeMatch<AbstractSpan>[] allMatches = Stream.concat(Stream.of(match1, match2), Stream.of(matches)).toArray(TraceNodeMatch[]::new);
        return new AndSpanMatch<>(allMatches);
    }

    default AndSpanMatch<AbstractSpan> and(TraceNodeMatch<AbstractSpan> match){
        //noinspection unchecked
        return new AndSpanMatch<>(this, match);
    }
}
