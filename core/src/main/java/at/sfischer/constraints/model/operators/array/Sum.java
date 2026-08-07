package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;

public class Sum extends ArrayAggregation {

    private static final String FUNCTION_NAME = "arrays.sum";

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
    protected ArrayAggregation createArrayAggregation(Node array, Node keySelector, Node... parameters) {
        return new Sum(
                array,
                keySelector
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
