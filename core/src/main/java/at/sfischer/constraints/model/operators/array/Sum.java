package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.*;

public class Sum extends ArrayOperation {

    private static final String FUNCTION_NAME = "arrays.sum";

    private Type arrayElementType = TypeEnum.ANY;

    public Sum(Node array) {
        this(array, new Variable(ELEMENT_NAME));
    }

    public Sum(Node array, Node keySelector) {
        super(FUNCTION_NAME, array, keySelector);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node keySelector = getParameter(1);

        if (arrayValues == null) {
            return this;
        }

        arrayElementType = arrayValues.getElementType();

        double sum = 0;
        boolean integer = true;
        for (Value<?> element : arrayValues.getValue()) {
            Node key = keySelector
                    .setVariableNameValue(ELEMENT_NAME, element)
                    .evaluate();

            if (!(key instanceof Value<?> value)) {
                return this;
            }

            if (value.getReturnType() == TypeEnum.INTEGER) {
                sum += ((Number) value.getValue()).longValue();
            } else if (value.getReturnType() == TypeEnum.NUMBER) {
                sum += ((Number) value.getValue()).doubleValue();
                integer = false;
            } else {
                return this;
            }
        }

        if (integer) {
            return new IntegerLiteral((int) sum);
        }

        return new NumberLiteral(sum);
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

            context.error(this, "Sum key selector must return INTEGER or NUMBER.");
        }

        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return;
        }

        arrayElementType = arrayValues.getElementType();

        Type elementVariableType;
        if(keySelector.equals(new Variable(ELEMENT_NAME))) {
            elementVariableType = TypeEnum.ANY;
        } else {
            Map<Variable, Type> variableTypes = keySelector.inferVariableTypes();
            elementVariableType = variableTypes.get(new Variable(ELEMENT_NAME));
            if (elementVariableType == null) {
                context.error(this, "Sum key selector must reference the array element variable.");
                return;
            }
        }

        if (!arrayElementType.canAssignTo(elementVariableType)) {
            context.error(this, "Sum key selector expects incompatible array element type.");
        }
    }

    @Override
    public List<Node> getChildren() {
        return List.of(
                getParameter(0),
                getParameter(1)
        );
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        Node array = getParameter(0);
        Node selector = getParameter(1);

        Map<Variable, Node> newValues = new HashMap<>(values);
        newValues.remove(new Variable(ELEMENT_NAME));

        return new Sum(
                array.setVariableValues(values),
                selector.setVariableValues(newValues)
        );
    }

    @Override
    public Node cloneNode() {
        return new Sum(
                getParameter(0).cloneNode(),
                getParameter(1).cloneNode()
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
        Node selector = getParameter(1);
        Type type = selector != null ? selector.getReturnType() : TypeEnum.NUMBER;
        if (type == TypeEnum.INTEGER) {
            return TypeEnum.INTEGER;
        }

        return TypeEnum.NUMBER;
    }
}
