package at.sfischer.traces.otel.testing;

import org.json.JSONArray;
import org.json.JSONObject;

public abstract class JsonPathStepMatch<NODE> {

    protected JsonPathStepMatch<?> nextStep;

    protected  <NEXT_NODE> JsonPathStepMatch<NEXT_NODE> linkNext(JsonPathStepMatch<NEXT_NODE> next) {
        this.nextStep = next;
        return next;
    }

    public abstract <RESPONSE, NEXT_NODE> MatchResult match(NODE current, Match<RESPONSE> match);

    @SuppressWarnings("unchecked")
    public <RESPONSE, NEXT_NODE> MatchResult matchNext(NEXT_NODE val, Match<RESPONSE> match) {
        if(this.nextStep == null){
            return match.matches((RESPONSE)val);
        } else {
            return ((JsonPathStepMatch<NEXT_NODE>)this.nextStep).match(val, match);
        }
    }

    public static JsonPathStepMatch<?> jsonPath(JsonPathStepMatch<?>... steps){
        if(steps.length == 0){
            return null;
        }
        JsonPathStepMatch<?> first = steps[0];
        JsonPathStepMatch<?> current = first;
        for (int i = 1; i < steps.length; i++) {
            current = current.linkNext(steps[i]);
        }

        return first;
    }

    public static JsonObjectStepMatch jsonObject(String fieldName){
        return new JsonObjectStepMatch(fieldName);
    }

    public static JsonArrayStepMatch jsonArray(int index){
        return new JsonArrayStepMatch(index);
    }

    public static JsonArrayMatchExists contains(){
        return new JsonArrayMatchExists();
    }

    public JsonPathStepMatch<JSONObject> json(String fieldName){
        return this.linkNext(jsonObject(fieldName));
    }

    public JsonPathStepMatch<JSONArray> json(int index){
        return this.linkNext(jsonArray(index));
    }
}