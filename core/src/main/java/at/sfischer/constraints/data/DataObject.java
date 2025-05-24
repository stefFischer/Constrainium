package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;
import org.javatuples.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.*;

import static at.sfischer.constraints.data.Utils.*;

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

    public void putValue(String name, DataValue<?>[] value) {
        Type nestedArrayType = value[0].getType();
        dataValues.put(name, new DataValue<>(new ArrayType(nestedArrayType), value));
    }

    protected void putDataValue(String name, DataValue<?> value) {
        dataValues.put(name, value);
    }

    public void putDataValues(DataObject object) {
        dataValues.putAll(object.dataValues);
    }

    public DataValue<?> getDataValue(String name){
        return dataValues.get(name);
    }

    public List<Value<?>> getValues(String name){
        int firstDotIndex = name.indexOf('.');
        String dataValueName = name;
        String rest = null;
        if (firstDotIndex != -1) {
            dataValueName = name.substring(0, firstDotIndex);
            rest = name.substring(firstDotIndex + 1);
        }

        DataValue<?> value = dataValues.get(dataValueName);
        if(value == null){
            return null;
        }

        List<Value<?>> values = new LinkedList<>();
        if(rest == null){
            values.add(value.getLiteralValue());
            return values;
        }

        Type valueType = value.getType();
        if(valueType == TypeEnum.COMPLEXTYPE){
            DataObject val = (DataObject)value.getValue();
            return val.getValues(rest);
        } else if(valueType instanceof ArrayType && ((ArrayType)valueType).elementType() == TypeEnum.COMPLEXTYPE){
            DataObject[] vals = (DataObject[])value.getValue();
            for (DataObject val : vals) {
                List<Value<?>> nestedValues = val.getValues(rest);
                if(nestedValues != null){
                    values.addAll(nestedValues);
                }
            }
            if(values.isEmpty()){
                return null;
            }

            return values;
        }

        return null;
    }

    public List<Value<?>> getValues(Path path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String currentSegment = path.segment(0);
        DataValue<?> value = dataValues.get(currentSegment);
        if (value == null) {
            return null;
        }

        if (path.size() == 1) {
            return List.of(value.getLiteralValue());
        }

        Path remainingPath = new Path(path.segments().subList(1, path.size()));
        List<Value<?>> values = new LinkedList<>();

        Type valueType = value.getType();
        if (valueType == TypeEnum.COMPLEXTYPE) {
            DataObject nested = (DataObject) value.getValue();
            return nested.getValues(remainingPath);
        } else if(valueType instanceof ArrayType && ((ArrayType)valueType).elementType() == TypeEnum.COMPLEXTYPE){
            DataObject[] nestedArray = (DataObject[]) value.getValue();
            for (DataObject nested : nestedArray) {
                List<Value<?>> nestedValues = nested.getValues(remainingPath);
                if (nestedValues != null) {
                    values.addAll(nestedValues);
                }
            }
            return values.isEmpty() ? null : values;
        }

        return null;
    }

    public Set<String> getFieldNames() {
        return this.dataValues.keySet();
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

    public Map<Type, List<Pair<String, DataValue<?>>>> getValuesByType(){
        Map<Type, List<Pair<String, DataValue<?>>>> dataByTypes = new HashMap<>();
        for (Map.Entry<String, DataValue<?>> entry : dataValues.entrySet()) {
            Type type = entry.getValue().getType();
            List<Pair<String, DataValue<?>>> data = dataByTypes.computeIfAbsent(type, k -> new LinkedList<>());
            String fieldName = entry.getKey();
            DataValue<?> value = entry.getValue();
            data.add(new Pair<>(fieldName, value));

            if(value.getValue() instanceof DataObject){
                Map<Type, List<Pair<String, DataValue<?>>>> internalDataByTypes = ((DataObject) value.getValue()).getValuesByType();
                for (Map.Entry<Type, List<Pair<String, DataValue<?>>>> internalEntry : internalDataByTypes.entrySet()) {
                    Type internalType = internalEntry.getKey();
                    List<Pair<String, DataValue<?>>> data2 = dataByTypes.computeIfAbsent(internalType, k -> new LinkedList<>());
                    for (Pair<String, DataValue<?>> pair : internalEntry.getValue()) {
                        String internalFieldName = pair.getValue0();
                        DataValue<?> internalValue = pair.getValue1();
                        data2.add(new Pair<>(fieldName + "." + internalFieldName, internalValue));
                    }
                }
            }
        }

        return dataByTypes;
    }

    public Map<String, Type> getDataTypes(){
        Map<String, Type> dataTypes = new HashMap<>();
        for (Map.Entry<String, DataValue<?>> entry : dataValues.entrySet()) {
            Map<String, Type> types = entry.getValue().getDataTypes();
            for (Map.Entry<String, Type> typeEntry : types.entrySet()) {
                if(typeEntry.getKey().isEmpty()){
                    dataTypes.put(entry.getKey(), typeEntry.getValue());
                } else {
                    if(!(entry.getValue().getType() instanceof ArrayType)) {
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
                    if(!(entry.getValue().getType() instanceof ArrayType)) {
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
        // Infer the element type pf the array.
        for (Object value : array) {
            elementType = inferElementType(elementType, value);
            values.add(value);
        }

        // Insert value array into data object.
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
            DataValue<?>[] value = new DataValue[values.size()];
            for (int i = 0; i < values.size(); i++) {
                DataObject valueDao = new DataObject();
                parseArray(valueDao, key, (JSONArray)values.get(i));
                DataValue<?> dataValue = valueDao.dataValues.get(key);
                value[i] = dataValue;
            }
            dao.putValue(key, value);
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
