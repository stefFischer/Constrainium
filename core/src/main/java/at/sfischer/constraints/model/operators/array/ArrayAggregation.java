package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ArrayAggregation extends ArrayOperation {

    protected Type arrayElementType = TypeEnum.ANY;

    public ArrayAggregation(String name, Node array, Node keySelector, Node... parameters) {
        super(name, combineParameters(array, keySelector, parameters));
    }

    private static Node[] combineParameters(Node array, Node keySelector, Node... parameters) {
        Node[] args = new Node[parameters.length + 2];
        args[0] = array;
        args[1] = keySelector;
        System.arraycopy(parameters, 0, args, 2, parameters.length);
        return args;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        Node array = getParameter(0).evaluate();
        Node keySelector = getParameter(1);

        if (array.getReturnType() == TypeEnum.ANY) {
            return;
        }

        Type returnType = keySelector.getReturnType();
        if (returnType != TypeEnum.ANY &&
                returnType != TypeEnum.INTEGER &&
                returnType != TypeEnum.NUMBER) {

            context.error(this, "Aggregation operation key selector must return INTEGER or NUMBER.");
        }

        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return;
        }

        Type elementVariableType;
        if(keySelector.equals(new Variable(ELEMENT_NAME))) {
            elementVariableType = TypeEnum.ANY;
        } else {
            Map<Variable, Type> variableTypes = keySelector.inferVariableTypes();
            elementVariableType = variableTypes.get(new Variable(ELEMENT_NAME));
            if (elementVariableType == null) {
                context.error(this, "Aggregation operation key selector must reference the array element variable.");
                return;
            }
        }

        if (!arrayElementType.canAssignTo(elementVariableType)) {
            context.error(this, "Aggregation operation key selector expects an incompatible array element type.");
        }
    }

    protected abstract ArrayAggregation createArrayAggregation(Node array, Node keySelector, Node... parameters);

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        Node array = getParameter(0);
        Node selector = getParameter(1);

        Map<Variable, Node> newValues = new HashMap<>(values);
        newValues.remove(new Variable(ELEMENT_NAME));

        return createArrayAggregation(
                array.setVariableValues(values),
                selector.setVariableValues(newValues)
        );
    }

    @Override
    public Node cloneNode() {
        return createArrayAggregation(
                getParameter(0).cloneNode(),
                getParameter(1).cloneNode()
        );
    }

    @Override
    public Map<Variable, Type> inferVariableTypes() {
        Node array = getParameter(0);
        Node keySelector = getParameter(1);
        Map<Variable, Type> keySelectorTypeMap = super.inferVariableTypes(array);
        Type elType = keySelectorTypeMap.remove(new Variable(ELEMENT_NAME));
        if(elType != null){
            arrayElementType = elType;
        }
        keySelectorTypeMap = super.inferVariableTypes(array);
        keySelectorTypeMap.remove(new Variable(ELEMENT_NAME));

        Map<Variable, Type> variableTypeMap = super.inferVariableTypes(keySelector);
        variableTypeMap.putAll(keySelectorTypeMap);

        return variableTypeMap;
    }

    @Override
    public List<Node> getChildren() {
        return List.of(
                getParameter(0),
                getParameter(1)
        );
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(
                new ArrayType(arrayElementType),
                TypeEnum.NUMBER
        );
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.NUMBER;
    }
}
