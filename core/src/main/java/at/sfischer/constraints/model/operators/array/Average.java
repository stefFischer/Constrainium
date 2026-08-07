package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;

public class Average extends ArrayAggregation {

    private static final String FUNCTION_NAME = "arrays.average";

    public Average(Node array) {
        this(array, new Variable(ELEMENT_NAME));
    }

    public Average(Node array, Node keySelector) {
        super(FUNCTION_NAME, array, keySelector);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node keySelector = getParameter(1);

        if (arrayValues == null) {
            return this;
        }

        Value<?>[] elements = arrayValues.getValue();
        if (elements.length == 0) {
            return new NumberLiteral(0);
        }

        double sum = 0;
        for (Value<?> element : elements) {
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
            } else {
                return this;
            }
        }

        return new NumberLiteral(sum / elements.length);
    }

    @Override
    protected ArrayAggregation createArrayAggregation(Node array, Node keySelector, Node... parameters) {
        return new Average(
                array,
                keySelector
        );
    }
}
