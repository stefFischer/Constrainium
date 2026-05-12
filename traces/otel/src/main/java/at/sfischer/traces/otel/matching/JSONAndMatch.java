package at.sfischer.traces.otel.matching;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONAndMatch<NODE> extends JsonPathStepMatch<NODE> {

    private final JSONMatch[] matches;

    public JSONAndMatch(JSONMatch... matches) {
        this.matches = matches;
    }

    @Override
    public <RESPONSE, NEXT_NODE> MatchResult match(NODE current, Match<RESPONSE> internalMatch) {
        String jsonString;
        if(current instanceof JSONObject){
            jsonString = current.toString();
        } else if(current instanceof JSONArray){
            jsonString = current.toString();
        } else {
            return new FailResult("JSON value of unexpected type: \"" + current.getClass() + "\".");
        }

        for (JSONMatch match : matches) {
            MatchResult result = match.matches(jsonString);
            if(result != MatchResult.SUCCESS){
                return result;
            }
        }

        return MatchResult.SUCCESS;
    }
}
