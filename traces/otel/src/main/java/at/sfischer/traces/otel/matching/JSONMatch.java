package at.sfischer.traces.otel.matching;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public abstract class JSONMatch implements Match<String> {

    @Deprecated
    public static JSONObject parseJson(String json){
        JSONTokener tokenizer = new JSONTokener(json);
        try{
            return new JSONObject(tokenizer);
        } catch (org.json.JSONException e){
            return null;
        }
    }

    @Deprecated
    public static <T> T getFieldValue(String json, String[] fieldPath){
        JSONObject jsonObject = parseJson(json);
        return getFieldValue(jsonObject, fieldPath);
    }

    @Deprecated
    public static <T> T getFieldValue(JSONObject json, String[] fieldPath){
        if(json == null){
            return null;
        }

        JSONObject currentObj = json;
        for (int i = 0; i < fieldPath.length; i++) {
            String field = fieldPath[i];
            Object val;
            try {
                val = currentObj.get(field);
            } catch (JSONException e){
                return null;
            }

            if(i == fieldPath.length - 1){
                //noinspection unchecked
                return (T)val;
            }

            if(val instanceof JSONObject){
                currentObj = (JSONObject)val;
                continue;
            }
            while(val instanceof JSONArray){
                i++;
                field = fieldPath[i];
                int index = Integer.parseInt(field);
                val = ((JSONArray) val).get(index);

                if(i == fieldPath.length - 1){
                    //noinspection unchecked
                    return (T)val;
                }

                if(val instanceof JSONObject){
                    currentObj = (JSONObject)val;
                }
            }
        }

        return null;
    }

    @Override
    public MatchResult matches(String json) {
        JSONTokener tokenizer = new JSONTokener(json);
        try{
            if(json.startsWith("[")){
                JSONArray array = new JSONArray(tokenizer);
                return matches(array);
            } else {
                JSONObject object = new JSONObject(tokenizer);
                return matches(object);
            }
        } catch (org.json.JSONException e){
            throw new IllegalArgumentException("Could not parse Json: " + json + ".", e);
        }
    }

    public abstract MatchResult matches(JSONObject json);

    public abstract MatchResult matches(JSONArray json);
}
