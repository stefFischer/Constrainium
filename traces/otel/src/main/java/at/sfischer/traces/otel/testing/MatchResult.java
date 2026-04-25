package at.sfischer.traces.otel.testing;

public interface MatchResult {

    MatchResult SUCCESS = SuccessResult.INSTANCE;

    String resultMessage();
}
