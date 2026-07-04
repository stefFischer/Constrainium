package at.sfischer.traces.otel.matching;

import at.sfischer.traces.otel.Span;

import java.util.stream.Stream;

public interface SpanMatch extends TraceNodeMatch<Span> {

    static TracerMatch tracer(String tracer){
        return new TracerMatch(tracer);
    }

    static TracerRegexMatch tracerRegex(String regex){
        return new TracerRegexMatch(regex);
    }

    static NameMatch<Span> name(String name){
        return new NameMatch<>(name);
    }

    static NameRegexMatch<Span> nameRegex(String regex){
        return new NameRegexMatch<>(regex);
    }

    static KindMatch<Span> kind(String kind){
        return new KindMatch<>(kind);
    }

    static KindRegexMatch<Span> kindRegex(String regex){
        return new KindRegexMatch<>(regex);
    }

    static AttributeExistsMatch<Span> attributeExists(String attributeName){
        return new AttributeExistsMatch<>(attributeName);
    }

    static AttributeRegexMatch<Span> attributeRegex(String attributeName, String valueRegex){
        return new AttributeRegexMatch<>(attributeName, valueRegex);
    }

    static <T> AttributeMatch<T, Span> attribute(String attributeName, T value){
        return new AttributeMatch<>(attributeName, value);
    }

    static AttributeJsonMatch<Span> attributeJson(String attributeName, JSONMatch... jsonMatches){
        return new AttributeJsonMatch<>(attributeName, jsonMatches);
    }

    static <T> JSONFieldValueMatch<T> json(T value, JsonPathStepMatch<Span>... steps){
        return new JSONFieldValueMatch<>(JsonPathStepMatch.jsonPath(steps), value);
    }

    static JSONMultipleMatch json(JsonPathStepMatch<Span> pathSteps, JSONMatch... matches){
        return new JSONMultipleMatch(pathSteps, matches);
    }

    static AndSpanMatch<Span> and(TraceNodeMatch<Span> match1, TraceNodeMatch<Span> match2, TraceNodeMatch<Span>... matches){
        //noinspection unchecked
        TraceNodeMatch<Span>[] allMatches = Stream.concat(Stream.of(match1, match2), Stream.of(matches)).toArray(TraceNodeMatch[]::new);
        return new AndSpanMatch<>(allMatches);
    }

    default AndSpanMatch<Span> and(TraceNodeMatch<Span> match){
        //noinspection unchecked
        return new AndSpanMatch<>(this, match);
    }
}
