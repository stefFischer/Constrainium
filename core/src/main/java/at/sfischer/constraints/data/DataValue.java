package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;

import java.lang.reflect.Array;
import java.util.*;

public class DataValue<T> {

    private final Type type;

    private final T value;

    protected DataValue(Type type, T value) {
        this.type = type;
        this.value = value;
        validate();
    }

    private void validate(){
        if (type == TypeEnum.NUMBER) {
            if(value instanceof Number){
                return;
            }
        } else if (type == TypeEnum.BOOLEAN) {
            if(value instanceof Boolean){
                return;
            }
        } else if (type == TypeEnum.STRING) {
            if(value instanceof String){
                return;
            }
        } else if (type instanceof ArrayType) {
            if(value.getClass().isArray()){
                return;
            }
        } else if (type == TypeEnum.COMPLEXTYPE) {
            if(value instanceof DataObject){
                return;
            }
        }

        throw new IllegalStateException("DataValue type (" + type + ") and value (" + value.getClass() + ") to not match.");
    }

    public Type getType() {
        return type;
    }

    public T getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataValue<?> dataValue = (DataValue<?>) o;
        return Objects.equals(type, dataValue.type) && Objects.equals(value, dataValue.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    public Map<String, Type> getDataTypes(){
        Map<String, Type> dataTypes = new HashMap<>();
        if (type == TypeEnum.COMPLEXTYPE) {
            dataTypes.putAll(((DataObject)value).getDataTypes());
        } else if (type instanceof ArrayType && ((ArrayType) type).elementType() == TypeEnum.COMPLEXTYPE) {
            DataObject[] val = (DataObject[]) value;
            for (DataObject dataObject : val) {
                Map<String, Type> elementTypes = dataObject.getDataTypes();
                for (Map.Entry<String, Type> entry : elementTypes.entrySet()) {
                    dataTypes.put(entry.getKey(), new ArrayType(entry.getValue()));
                }
            }
        }

        dataTypes.put("", type);
        return dataTypes;
    }

    public Map<String, Node> getDataValues() {
        Map<String,  Node> dataValues = new HashMap<>();
        if (type == TypeEnum.COMPLEXTYPE) {
            dataValues.putAll(((DataObject) value).getDataValues());
        } else if (type instanceof ArrayType && ((ArrayType) type).elementType() == TypeEnum.COMPLEXTYPE) {
            DataObject[] val = (DataObject[]) value;
            Map<String, List<Node>> arrayValues = new HashMap<>();
            for (DataObject dataObject : val) {
                Map<String, Node> elementValues = dataObject.getDataValues();
                for (Map.Entry<String, Node> entry : elementValues.entrySet()) {
                    List<Node> literals = arrayValues.computeIfAbsent(entry.getKey(), k -> new LinkedList<>());
                    literals.add(entry.getValue());
                }
            }
            for (Map.Entry<String, List<Node>> entry : arrayValues.entrySet()) {
                dataValues.put(entry.getKey(), ArrayValues.createArrayValuesFromList(entry.getValue()));
            }
        }

        dataValues.put("", getLiteralValue());
        return dataValues;
    }

    public Value<?> getLiteralValue() {
        return getLiteralValue(type, value);
    }

    public static <T> Value<?> getLiteralValue(Type type, T value){
        if (type == TypeEnum.NUMBER) {
            return new NumberLiteral((Number) value);
        } else if (type == TypeEnum.BOOLEAN) {
            return BooleanLiteral.getBooleanLiteral((Boolean) value);
        } else if (type == TypeEnum.STRING) {
            return new StringLiteral((String) value);
        } else if (type == TypeEnum.COMPLEXTYPE) {
            return new ComplexValue((DataObject) value);
        } else if (type instanceof ArrayType) {
            Type elementType = ((ArrayType) type).elementType();
            if (elementType == TypeEnum.NUMBER) {
                Number[] val =  (Number[])value;
                NumberLiteral[] literalValues = new NumberLiteral[val.length];
                for (int i = 0; i < val.length; i++) {
                    literalValues[i] = new NumberLiteral(val[i]);
                }

                return new ArrayValues<>(elementType, literalValues);
            } else if (elementType == TypeEnum.BOOLEAN) {
                boolean[] val =  (boolean[])value;
                BooleanLiteral[] literalValues = new BooleanLiteral[val.length];
                for (int i = 0; i < val.length; i++) {
                    literalValues[i] = BooleanLiteral.getBooleanLiteral(val[i]);
                }

                return new ArrayValues<>(elementType, literalValues);
            } else if (elementType == TypeEnum.STRING) {
                String[] val =  (String[])value;
                StringLiteral[] literalValues = new StringLiteral[val.length];
                for (int i = 0; i < val.length; i++) {
                    literalValues[i] = new StringLiteral(val[i]);
                }

                return new ArrayValues<>(elementType, literalValues);
            } else if (elementType == TypeEnum.COMPLEXTYPE) {
                DataObject[] val = (DataObject[]) value;
                ComplexValue[] array = new ComplexValue[val.length];
                for (int i = 0; i < val.length; i++) {
                    array[i] = new ComplexValue(val[i]);
                }

                return new ArrayValues<>(elementType, array);
            } else if (elementType instanceof ArrayType) {
                DataValue<?>[] val = (DataValue<?>[]) value;
                ArrayValues<?>[] array = new ArrayValues[val.length];
                for (int i = 0; i < val.length; i++) {
                    array[i] = (ArrayValues<?>)getLiteralValue(elementType, val[i].getValue());
                }

                return new ArrayValues<>(elementType, array);
            }
        }

        throw new IllegalStateException("Unsupported type: " + type);
    }

    @Override
    public String toString() {
        StringBuilder valueString;
        if(value.getClass().isArray()){
            valueString = new StringBuilder("[");
            for (int i = 0; i < Array.getLength(value); i++) {
                if(i > 0){
                    valueString.append(", ");
                }
                valueString.append(Array.get(value, i));
            }
            valueString.append("]");
        } else {
            valueString = new StringBuilder(value.toString());
        }

        return "DataValue{" +
                "type=" + type +
                ", value=" + valueString +
                '}';
    }
}
