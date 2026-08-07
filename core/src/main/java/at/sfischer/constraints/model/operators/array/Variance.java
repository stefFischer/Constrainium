package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;

public class Variance extends ArrayAggregation {

    private static final String FUNCTION_NAME = "arrays.variance";

    public Variance(Node array) {
        this(array, new Variable(ELEMENT_NAME));
    }

    public Variance(Node array, Node keySelector) {
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

        double[] values = new double[elements.length];
        double sum = 0;
        for (int i = 0; i < elements.length; i++) {
            Node key = keySelector
                    .setVariableNameValue(ELEMENT_NAME, elements[i])
                    .evaluate();

            if (!(key instanceof Value<?> value)) {
                return this;
            }

            double number;
            if (value.getReturnType() == TypeEnum.INTEGER) {
                number = ((Number) value.getValue()).longValue();
            } else if (value.getReturnType() == TypeEnum.NUMBER) {
                number = ((Number) value.getValue()).doubleValue();
            } else {
                return this;
            }

            values[i] = number;
            sum += number;
        }

        double mean = sum / values.length;
        double variance = 0;
        for (double value : values) {
            double diff = value - mean;
            variance += diff * diff;
        }

        variance /= values.length;

        return new NumberLiteral(variance);
    }

    @Override
    protected ArrayAggregation createArrayAggregation(Node array, Node keySelector, Node... parameters) {
        return new Variance(array, keySelector);
    }
}
