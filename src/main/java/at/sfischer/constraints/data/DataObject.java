package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.*;

public class DataObject {

    private final Map<String, DataValue<?>> dataValues;

    public DataObject() {
        this.dataValues = new HashMap<>();
    }

    public void putValue(String name, boolean value){
        dataValues.put(name, new DataValue<>(TypeEnum.BOOLEAN, value));
    }

    public void putValue(String name, Number value){
        dataValues.put(name, new DataValue<>(TypeEnum.NUMBER, value));
    }

    public void putValue(String name, String value){
        dataValues.put(name, new DataValue<>(TypeEnum.STRING, value));
    }

    public void putValue(String name, DataObject value){
        dataValues.put(name, new DataValue<>(TypeEnum.COMPLEXTYPE, value));
    }

    public void putValue(String name, boolean[] value){
        dataValues.put(name, new DataValue<>(new ArrayType(TypeEnum.BOOLEAN), value));
    }

    public void putValue(String name, Number[] value){
        dataValues.put(name, new DataValue<>(new ArrayType(TypeEnum.NUMBER), value));
    }

    public void putValue(String name, String[] value){
        dataValues.put(name, new DataValue<>(new ArrayType(TypeEnum.STRING), value));
    }

    public void putValue(String name, DataObject[] value){
        dataValues.put(name, new DataValue<>(new ArrayType(TypeEnum.COMPLEXTYPE), value));
    }

    public DataValue<?> getDataValue(String name){
        return dataValues.get(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataObject that = (DataObject) o;
        return Objects.equals(dataValues, that.dataValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataValues);
    }

    public Map<String, Type> getDataTypes(){
        Map<String, Type> dataTypes = new HashMap<>();
        for (Map.Entry<String, DataValue<?>> entry : dataValues.entrySet()) {
            Map<String, Type> types = entry.getValue().getDataTypes();
            for (Map.Entry<String, Type> typeEntry : types.entrySet()) {
                if(typeEntry.getKey().isEmpty()){
                    dataTypes.put(entry.getKey(), typeEntry.getValue());
                } else {
                    if(entry.getValue().getType() instanceof ArrayType) {
                        dataTypes.put(entry.getKey() + "[" + typeEntry.getKey() + "]", typeEntry.getValue());
                    } else {
                        dataTypes.put(entry.getKey() + "." + typeEntry.getKey(), typeEntry.getValue());
                    }
                }
            }
        }

        return dataTypes;
    }

    public Map<String, Node> getDataValues(){
        Map<String, Node> dataValues = new HashMap<>();
        for (Map.Entry<String, DataValue<?>> entry : this.dataValues.entrySet()) {
            Map<String, Node> values = entry.getValue().getDataValues();
            for (Map.Entry<String, Node> typeEntry : values.entrySet()) {
                if(typeEntry.getKey().isEmpty()){
                    dataValues.put(entry.getKey(), typeEntry.getValue());
                } else {
                    if(entry.getValue().getType() instanceof ArrayType) {
                        dataValues.put(entry.getKey() + "[" + typeEntry.getKey() + "]", typeEntry.getValue());
                    } else {
                        dataValues.put(entry.getKey() + "." + typeEntry.getKey(), typeEntry.getValue());
                    }
                }
            }
        }

        return dataValues;
    }

    @Override
    public String toString() {
        return "DataObject{" +
                "dataValues=" + dataValues +
                '}';
    }

    public static DataObject parseData(String jsonData){
        DataObject dao = new DataObject();

        JSONTokener jt = new JSONTokener(jsonData);
        JSONObject object = new JSONObject(jt);
        parseObject(dao, object);

        return dao;
    }

    private static void parseObject(DataObject dao, JSONObject object){
        Iterator<String> keyIt = object.keys();
        while(keyIt.hasNext()){
            String key = keyIt.next();
            Object value = object.get(key);

            if(value instanceof Number){
                dao.putValue(key, (Number)value);
            } else if (value instanceof Boolean){
                dao.putValue(key, (Boolean)value);
            } else if (value instanceof String) {
                dao.putValue(key, (String)value);
            } else if (value instanceof JSONArray) {
                parseArray(dao, key, (JSONArray)value);
            } else if (value instanceof JSONObject) {
                DataObject valueDao = new DataObject();
                parseObject(valueDao, (JSONObject)value);
                dao.putValue(key, valueDao);
            }
        }
    }

    private static void parseArray(DataObject dao, String key, JSONArray array){
        Class<?> elementType = null;
        List<Object> values = new ArrayList<>();
        for (Object value : array) {
            if(value instanceof Number){
                elementType = inferElementType(elementType, value);
                values.add(value);
            } else if (value instanceof Boolean){
                elementType = inferElementType(elementType, value);
                values.add(value);
            } else if (value instanceof String) {
                elementType = inferElementType(elementType, value);
                values.add(value);
            } else if (value instanceof JSONArray) {
                elementType = inferElementType(elementType, value);

                // TODO Support nested array.
                System.err.println("NESTED ARRAY NOT YET SUPPORTED");

            } else if (value instanceof JSONObject) {
                elementType = inferElementType(elementType, value);
                DataObject valueDao = new DataObject();
                parseObject(valueDao, (JSONObject)value);
                values.add(value);
            }
        }

        if(elementType == Number.class){
            Number[] value = new Number[values.size()];
            for (int i = 0; i < values.size(); i++) {
                value[i] = (Number)values.get(i);
            }
            dao.putValue(key, value);
        } else if(elementType == Boolean.class){
            boolean[] value = new boolean[values.size()];
            for (int i = 0; i < values.size(); i++) {
                value[i] = (boolean)values.get(i);
            }
            dao.putValue(key, value);
        } else if(elementType == String.class){
            String[] value = new String[values.size()];
            for (int i = 0; i < values.size(); i++) {
                value[i] = (String)values.get(i);
            }
            dao.putValue(key, value);
        } else if(elementType == JSONArray.class){
            // TODO Support nested array.

        } else if(elementType == Object.class){
            DataObject[] value = new DataObject[values.size()];
            for (int i = 0; i < values.size(); i++) {
                DataObject valueDao = new DataObject();
                parseObject(valueDao, (JSONObject)values.get(i));
                value[i] = valueDao;
            }
            dao.putValue(key, value);
        }

    }

    private static Class<?> inferElementType(Class<?> elementType, Object value){
        if(value instanceof Number){
            if(elementType == null){
                return Number.class;
            } else if(!elementType.equals(Number.class)) {
                return Object.class;
            }
        } else if (value instanceof Boolean){
            if(elementType == null){
                return Boolean.class;
            } else if(!elementType.equals(Boolean.class)) {
                return Object.class;
            }
        } else if (value instanceof String) {
            if(elementType == null){
                return String.class;
            } else if(!elementType.equals(String.class)) {
                return Object.class;
            }
        } else if (value instanceof JSONArray) {
            if(elementType == null){
                return JSONArray.class;
            } else if(!elementType.equals(JSONArray.class)) {
                return Object.class;
            }
        } else if (value instanceof JSONObject) {
            return Object.class;
        }

        return elementType;
    }
}
