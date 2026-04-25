package at.sfischer.traces.otel.testing;

public interface Match<T> {

    MatchResult matches(T toCheck);
}
