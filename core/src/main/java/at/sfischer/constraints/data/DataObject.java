package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;
import org.javatuples.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.*;

import static at.sfischer.constraints.data.Utils.Path;

public class DataObject {

    private final Map<String, DataValue<?>> dataValues;

    public DataObject() {
        this.dataValues = new HashMap<>();
    }

    public void putValue(String path, boolean value){
        putDataValue(path, new DataValue<>(TypeEnum.BOOLEAN, value));
    }

    public void putValue(String path, Integer value){
        putDataValue(path, new DataValue<>(TypeEnum.INTEGER, value));
    }

    public void putValue(String path, Number value){
        putDataValue(path, new DataValue<>(TypeEnum.NUMBER, value));
    }

    public void putValue(String path, String value){
        putDataValue(path, new DataValue<>(TypeEnum.STRING, value));
    }

    public void putValue(String path, DataObject value){
        putDataValue(path, new DataValue<>(TypeEnum.COMPLEXTYPE, value));
    }

    public void putValue(String path, boolean[] value){
        putDataValue(path, new DataValue<>(new ArrayType(TypeEnum.BOOLEAN), value));
    }

    public void putValue(String path, Boolean[] value){
        putDataValue(path, new DataValue<>(new ArrayType(TypeEnum.BOOLEAN), value));
    }

    public void putValue(String path, Integer[] value){
        putDataValue(path, new DataValue<>(new ArrayType(TypeEnum.INTEGER), value));
    }

    public void putValue(String path, Number[] value){
        putDataValue(path, new DataValue<>(new ArrayType(TypeEnum.NUMBER), value));
    }

    public void putValue(String path, String[] value){
        putDataValue(path, new DataValue<>(new ArrayType(TypeEnum.STRING), value));
    }

    public void putValue(String path, DataObject[] value){
        putDataValue(path, new DataValue<>(new ArrayType(TypeEnum.COMPLEXTYPE), value));
    }

    protected void putValue(String name, DataValue<?>[] value) {
        Type nestedArrayType = value[0].getType();
        putValue(name, value, nestedArrayType);
    }

    public void putValue(String path, DataValue<?>[] value, Type nestedArrayType) {
        putDataValue(path, new DataValue<>(new ArrayType(nestedArrayType), value));
    }

    public void putDataValues(DataObject object) {
        dataValues.putAll(object.dataValues);
    }

    protected void putDataValue(String path, DataValue<?> value) {
        DataObject target = getObjectForPath(path);
        target.dataValues.put(lastPathElement(path), value);
    }

    public DataValue<?> getDataValue(String name){
        return dataValues.get(name);
    }

    private DataObject getOrCreateObject(String name) {
        DataValue<?> value = dataValues.get(name);
        if (value != null && value.getType() == TypeEnum.COMPLEXTYPE) {
            return (DataObject) value.getValue();
        }

        DataObject object = new DataObject();
        putValue(name, object);
        return object;
    }

    private DataObject getObjectForPath(String path) {
        String[] parts = path.split("\\.");
        DataObject current = this;
        for (int i = 0; i < parts.length - 1; i++) {
            current = current.getOrCreateObject(parts[i]);
        }

        return current;
    }

    private static String lastPathElement(String path) {
        int index = path.lastIndexOf('.');
        return index < 0 ? path : path.substring(index + 1);
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
    public DataObject clone() {
        DataObject clone = new DataObject();

        for (Map.Entry<String, DataValue<?>> entry : dataValues.entrySet()) {
            clone.putDataValue(entry.getKey(), entry.getValue().clone());
        }

        return clone;
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

    public void putNodeValue(String path, Value<?> value) {
        Object converted = toDataValue(value);

        if (value.getReturnType() instanceof ArrayType(Type elementType)) {
            putValue(path, (DataValue<?>[]) converted, elementType);
        } else if (converted instanceof Boolean b) {
            putValue(path, b);
        } else if (converted instanceof Integer i) {
            putValue(path, i);
        } else if (converted instanceof Number n) {
            putValue(path, n);
        } else if (converted instanceof String s) {
            putValue(path, s);
        } else if (converted instanceof DataObject d) {
            putValue(path, d);
        } else {
            throw new IllegalStateException("Unsupported value type: " + value.getClass());
        }
    }

    private static Object toDataValue(Value<?> value) {
        if (value instanceof BooleanLiteral bl) {
            return bl.getValue();
        }

        if (value instanceof IntegerLiteral il) {
            return il.getValue();
        }

        if (value instanceof NumberLiteral nl) {
            return nl.getValue();
        }

        if (value instanceof StringLiteral sl) {
            return sl.getValue();
        }

        if (value instanceof ComplexValue cv) {
            return cv.getValue().clone();
        }

        if (value instanceof ArrayValues<?> av) {
            DataValue<?>[] result = new DataValue<?>[av.getValue().length];
            for (int i = 0; i < result.length; i++) {
                Value<?> element = av.getValue()[i];
                result[i] = new DataValue<>(
                        element.getReturnType(),
                        toDataValue(element)
                );
            }

            return result;
        }

        throw new IllegalStateException("Unsupported value node: " + value.getClass());
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

            if(value instanceof Integer){
                dao.putValue(key, (Integer)value);
            } else if(value instanceof Number){
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
        if(elementType == Integer.class){
            Integer[] value = new Integer[values.size()];
            for (int i = 0; i < values.size(); i++) {
                value[i] = (Integer)values.get(i);
            }
            dao.putValue(key, value);
        } else if(elementType == Number.class){
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
        if(value instanceof Integer){
            if(elementType == null){
                return Integer.class;
            } else if(!elementType.equals(Integer.class)) {
                return Object.class;
            }
        } else if(value instanceof Number){
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
