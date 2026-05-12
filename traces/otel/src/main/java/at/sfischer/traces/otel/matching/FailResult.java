package at.sfischer.traces.otel.matching;

public class FailResult implements MatchResult {

    private final String message;

    public FailResult(String message) {
        this.message = message;
    }

    @Override
    public String resultMessage() {
        return message;
    }
}
