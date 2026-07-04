package at.sfischer.traces.otel.matching;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONFieldValueMatch<T> extends JSONMatch {

    private final JsonPathStepMatch<?> pathSteps;

    private final T expectedFieldValue;

    public JSONFieldValueMatch(JsonPathStepMatch<?> pathSteps, T expectedFieldValue) {
        this.pathSteps = pathSteps;
        this.expectedFieldValue = expectedFieldValue;
    }

    @Override
    public MatchResult matches(JSONObject json) {
        try {
            //noinspection unchecked
            return ((JsonPathStepMatch<JSONObject>)pathSteps).match(json, new ObjectEqualsMatch<>(expectedFieldValue));
        } catch (ClassCastException e){
            return new FailResult("Unexpected type of path step: " + pathSteps + ".");
        }
    }

    @Override
    public MatchResult matches(JSONArray json) {
        try {
            //noinspection unchecked
            return ((JsonPathStepMatch<JSONArray>)pathSteps).match(json, new ObjectEqualsMatch<>(expectedFieldValue));
        } catch (ClassCastException e){
            return new FailResult("Unexpected type of path step: " + pathSteps + ".");
        }
    }
}
