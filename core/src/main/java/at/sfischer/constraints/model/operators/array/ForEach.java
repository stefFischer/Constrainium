package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForEach extends ArrayOperation {

    private static final String FUNCTION_NAME = "arrays.forEach";

    private final Type arrayElementType;

    public ForEach(Node array, Node operation) {
        super(FUNCTION_NAME, array, operation);
        this.arrayElementType = operation.getReturnType();
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node operation = getParameter(1);

        if(arrayValues != null && operation != null){
            List<Value<?>> resultValues = new ArrayList<>();
            Value<?>[] elements = arrayValues.getValue();
            for (Value<?> element : elements) {
                Node op = operation.setVariableNameValue(ELEMENT_NAME, element);
                Node result = op.evaluate();
                if(result instanceof Value<?> val && val.getReturnType().canAssignTo(arrayElementType)){
                    resultValues.add(val);
                } else {
                    return this;
                }
            }

            return new ArrayValues<>(arrayElementType, resultValues.toArray(new Value[0]));
        }

        return this;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        Node operation = getParameter(1);
        if(operation == null){
            context.error(this, "For each operation cannot be null.");
            return;
        }

        Map<Variable, Type> variableTypes = operation.inferVariableTypes();
        Type elementVariableType = variableTypes.get(new Variable(ELEMENT_NAME));
        if(elementVariableType == null){
            context.error(this, "For each operation needs to use the array elements 'ARRAY_ELEMENT' variable.");
            return;
        }

        Node array = getParameter(0).evaluate();
        if(array.getReturnType() == TypeEnum.ANY){
            return;
        }
        if(!(array instanceof ArrayValues<?> arrayValues)){
            return;
        }

        if(!arrayValues.getElementType().canAssignTo(elementVariableType)){
            context.error(this, "For each operation uses ARRAY_ELEMENT with an incompatible type.");
        }
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

        return new ForEach(newArray, newCondition);
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(TypeEnum.ANY), arrayElementType);
    }

    @Override
    public Type getReturnType() {
        return new ArrayType(arrayElementType);
    }
}
