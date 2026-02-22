package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ArrayQuantifier extends Function {
    public static final String ELEMENT_NAME = "ARRAY_ELEMENT";

    protected Type arrayElementType = TypeEnum.ANY;

    public ArrayQuantifier(String functionName, Node array, Node condition) {
        super(functionName, array, condition);
    }

    @Override
    public boolean validate() {
        if(!super.validate()){
            return false;
        }

        Node array = getParameter(0).evaluate();
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
        Node array = getParameter(0);
        Node condition = getParameter(1);

        Node newArray = array.setVariableValues(values);

        Map<Variable, Node> newValues = new HashMap<>(values);
        // The quantifiers set the element value directly on the condition, if an element identifier is passed here, there must be an outer quantifier and we should not pass it further into the condition.
        newValues.remove(new Variable(ELEMENT_NAME));
        Node newCondition = condition.setVariableValues(newValues);

        return createArrayQuantifier(newArray, newCondition);
    }

    @Override
    public Node cloneNode() {
        Node array = getParameter(0);
        Node condition = getParameter(1);
        return createArrayQuantifier(array.cloneNode(), condition.cloneNode());
    }

    protected abstract ArrayQuantifier createArrayQuantifier(Node array, Node condition);

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
        Node array = getParameter(0);
        Node condition = getParameter(1);
        Map<Variable, Type> conditionTypeMap = super.inferVariableTypes(array);
        arrayElementType = conditionTypeMap.remove(new Variable(ELEMENT_NAME));
        conditionTypeMap = super.inferVariableTypes(array);
        conditionTypeMap.remove(new Variable(ELEMENT_NAME));

        Map<Variable, Type> variableTypeMap = super.inferVariableTypes(condition);
        variableTypeMap.putAll(conditionTypeMap);

        return variableTypeMap;
    }

    @Override
    public String toString() {
        return getName() + "(array: " + getParameter(0) + ", condition: " + getParameter(1) + ")";
    }
}
