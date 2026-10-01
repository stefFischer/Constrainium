package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CombineArrays extends ArrayOperation {

    private static final String FUNCTION_NAME = "arrays.combine";

    public CombineArrays(Node left, Node right, Node operator) {
        super(FUNCTION_NAME, left, right, operator);
    }

    @Override
    public Node evaluate() {
        Node left = getParameter(0).evaluate();
        Node right = getParameter(1).evaluate();
        Node operator = getParameter(2);
        if (!(left instanceof ArrayValues<?> leftArray) || !(right instanceof ArrayValues<?> rightArray)) {
            return this;
        }

        Value<?>[] leftValues = leftArray.getValue();
        Value<?>[] rightValues = rightArray.getValue();
        if (leftValues.length != rightValues.length) {
            return this;
        }

        List<Value<?>> result = new ArrayList<>(leftValues.length);
        for (int i = 0; i < leftValues.length; i++) {
            Node expression = operator.setVariableNameValues(Map.of(
                    LEFT_ELEMENT_NAME, leftValues[i],
                    RIGHT_ELEMENT_NAME, rightValues[i],
                    INDEX_NAME, new IntegerLiteral(i)
            ));

            Node value = expression.evaluate();
            if (!(value instanceof Value<?> resultValue)) {
                return this;
            }

            result.add(resultValue);
        }

        Type elementType = operator.getReturnType();
        return new ArrayValues<>(elementType, result.toArray(new Value<?>[0]));
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        Node left = getParameter(0).evaluate();
        Node right = getParameter(1).evaluate();
        if (left.getReturnType() != TypeEnum.ANY && right.getReturnType() != TypeEnum.ANY) {
            if (!(left instanceof ArrayValues<?> leftArray) || !(right instanceof ArrayValues<?> rightArray)) {
                return;
            }

            if (leftArray.getValue().length != rightArray.getValue().length) {
                context.error(this, "Array combination requires arrays of equal length.");
            }
        }
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1), getParameter(2));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(TypeEnum.ANY), new ArrayType(TypeEnum.ANY), TypeEnum.ANY);
    }

    @Override
    public Type getReturnType() {
        Type operatorType = getParameter(2).getReturnType();
        return new ArrayType(operatorType);
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new CombineArrays(
                getParameter(0).setVariableValues(values),
                getParameter(1).setVariableValues(values),
                getParameter(2).setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new CombineArrays(
                getParameter(0).cloneNode(),
                getParameter(1).cloneNode(),
                getParameter(2).cloneNode()
        );
    }
}
