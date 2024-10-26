package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;

public abstract class ArrayQuantifier extends Function {
    public static final String ELEMENT_NAME = "ARRAY.ELEMENT";

    protected Type arrayElementType = TypeEnum.ANY;

    public ArrayQuantifier(String functionName, Node array, Node condition) {
        super(functionName, array, condition);
    }

    @Override
    public boolean validate() {
        if(!super.validate()){
            return false;
        }

        Node array = getParameter(0);
        if(!(array instanceof ArrayValues<?>)){
            return false;
        }

        Type elementType = ((ArrayValues<?>) array).getElementType();
        Node condition = getParameter(1);
        Map<Variable, Type> variableTypes = condition.inferVariableTypes();
        Type elementVariableType = variableTypes.get(new Variable(ELEMENT_NAME));
        return elementType.equals(elementVariableType);
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new ForAll(getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(arrayElementType), TypeEnum.BOOLEAN);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }

    @Override
    public Map<Variable, Type> inferVariableTypes() {
        Map<Variable, Type> variableTypeMap =  super.inferVariableTypes();
        arrayElementType = variableTypeMap.remove(new Variable(ELEMENT_NAME));
        variableTypeMap =  super.inferVariableTypes();
        variableTypeMap.remove(new Variable(ELEMENT_NAME));
        return variableTypeMap;
    }

    @Override
    public String toString() {
        return getName() + "(array: " + getParameter(0) + ", condition: " + getParameter(1) + ")";
    }
}
