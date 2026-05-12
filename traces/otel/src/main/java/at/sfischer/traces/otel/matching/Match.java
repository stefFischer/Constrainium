package at.sfischer.traces.otel.matching;

public interface Match<T> {

    MatchResult matches(T toCheck);
}
