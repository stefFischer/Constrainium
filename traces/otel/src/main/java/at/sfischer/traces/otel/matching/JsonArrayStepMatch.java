package at.sfischer.traces.otel.matching;

import org.json.JSONArray;
import org.json.JSONException;

public class JsonArrayStepMatch extends JsonPathStepMatch<JSONArray> {

    private final int index;

    public JsonArrayStepMatch(int index) {
        this.index = index;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <RESPONSE, NEXT_NODE> MatchResult match(JSONArray current, Match<RESPONSE> match) {
        try {
            NEXT_NODE val = (NEXT_NODE) current.get(index);
            return matchNext(val, match);
        } catch (JSONException e){
            return new FailResult("Could not find element at index: \"" + index + "\".");
        } catch (ClassCastException e){
            return new FailResult("Element at index: \"" + index + "\" has unexpected type.");
        }
    }
}
