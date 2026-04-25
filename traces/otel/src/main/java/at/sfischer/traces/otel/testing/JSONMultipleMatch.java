package at.sfischer.traces.otel.testing;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONMultipleMatch extends JSONMatch {

    private final JsonPathStepMatch<?> pathSteps;

    private final JSONMatch[] matches;

    public JSONMultipleMatch(JsonPathStepMatch<?> pathSteps, JSONMatch... matches) {
        this.pathSteps = pathSteps;
        this.matches = matches;
    }

    @Override
    public MatchResult matches(JSONObject json) {
        JsonPathStepMatch<?> current = pathSteps;
        while(current.nextStep != null){
            current = current.nextStep;
        }
        current.linkNext(new JSONAndMatch<>(matches));
        try {
            //noinspection unchecked
            return ((JsonPathStepMatch<JSONObject>)pathSteps).match(json, null);
        } catch (ClassCastException e){
            return new FailResult("Unexpected type of path step: " + pathSteps + ".");
        }
    }

    @Override
    public MatchResult matches(JSONArray json) {
        JsonPathStepMatch<?> current = pathSteps;
        while(current.nextStep != null){
            current = current.nextStep;
        }
        current.linkNext(new JSONAndMatch<>(matches));
        try {
            //noinspection unchecked
            return ((JsonPathStepMatch<JSONArray>)pathSteps).match(json, null);
        } catch (ClassCastException e){
            return new FailResult("Unexpected type of path step: " + pathSteps + ".");
        }
    }
}
