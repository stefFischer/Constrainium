package at.sfischer.traces.otel.matching;

import java.util.LinkedList;
import java.util.List;

public class AndMatch<T> implements Match<T> {

    private final Match<T>[] matches;

    public AndMatch(Match<T>... matches) {
        this.matches = matches;
    }

    @Override
    public MatchResult matches(T toCheck) {
        List<MatchResult> fails = new LinkedList<>();
        for (Match<T> match : matches) {
            MatchResult result = match.matches(toCheck);
            if(result != MatchResult.SUCCESS){
                fails.add(result);
            }
        }

        if(!fails.isEmpty()){
            return new FailsResult(fails);
        }

        return MatchResult.SUCCESS;
    }
}
