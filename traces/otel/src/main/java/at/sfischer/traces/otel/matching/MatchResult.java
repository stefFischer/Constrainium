package at.sfischer.traces.otel.matching;

public interface MatchResult {

    MatchResult SUCCESS = SuccessResult.INSTANCE;

    String resultMessage();
}
