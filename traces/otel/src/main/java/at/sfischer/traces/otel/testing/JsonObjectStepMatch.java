package at.sfischer.traces.otel.testing;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonObjectStepMatch extends JsonPathStepMatch<JSONObject> {

    private final String fieldName;

    public JsonObjectStepMatch(String fieldName) {
        this.fieldName = fieldName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <RESPONSE, NEXT_NODE> MatchResult match(JSONObject current, Match<RESPONSE> match) {
        try {
            NEXT_NODE val = (NEXT_NODE) current.get(fieldName);
            return matchNext(val, match);
        } catch (JSONException e){
            return new FailResult("Could not find field named: \"" + fieldName + "\".");
        } catch (ClassCastException e){
            return new FailResult("Field named: \"" + fieldName + "\" has unexpected type.");
        }
    }
}
