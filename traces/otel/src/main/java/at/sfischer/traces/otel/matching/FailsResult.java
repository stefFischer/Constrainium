package at.sfischer.traces.otel.matching;

import java.util.LinkedList;
import java.util.List;

public class FailsResult implements MatchResult {

    private final List<MatchResult> fails;

    public FailsResult(List<MatchResult> fails) {
        this.fails = new LinkedList<>(fails);
        this.fails.removeIf(fail -> fail == MatchResult.SUCCESS);
    }

    @Override
    public String resultMessage() {
        StringBuilder message = new StringBuilder();
        boolean first = true;
        for (MatchResult fail : fails) {
            if(!first){
                message.append("\n");
            }
            first = false;

            message.append(fail.resultMessage());
        }

        return message.toString();
    }
}
