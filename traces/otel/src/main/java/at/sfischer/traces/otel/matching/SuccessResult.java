package at.sfischer.traces.otel.matching;

public class SuccessResult implements MatchResult {

    public static final SuccessResult INSTANCE = new SuccessResult();

    public static final String MESSAGE = "Match Found.";

    private SuccessResult() {
    }

    @Override
    public String resultMessage() {
        return MESSAGE;
    }
}
