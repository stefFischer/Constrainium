package at.sfischer.traces.otel.matching;

import org.json.JSONArray;

public class JsonArrayMatchExists extends JsonPathStepMatch<JSONArray> {

    @Override
    public <RESPONSE, NEXT_NODE> MatchResult match(JSONArray current, Match<RESPONSE> match) {
        int length = current.length();
        for (int i = 0; i < length; i++) {
            JsonArrayStepMatch arrayStepMatch = new JsonArrayStepMatch(i);
            arrayStepMatch.linkNext(this.nextStep);
            if(arrayStepMatch.match(current, match) == MatchResult.SUCCESS){
                return MatchResult.SUCCESS;
            }
        }

        return new FailResult("Could not find any matching element.");
    }
}
