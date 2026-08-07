package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;

import java.util.Arrays;
import java.util.List;

public class Percentile extends ArrayAggregation {

    private static final String FUNCTION_NAME = "arrays.percentile";

    public Percentile(Node array, Node percentile) {
        this(array, new Variable(ELEMENT_NAME), percentile);
    }

    public Percentile(Node array, Node keySelector, Node percentile) {
        super(FUNCTION_NAME, array, keySelector, percentile);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node keySelector = getParameter(1);
        Node percentileNode = getParameter(2).evaluate();

        if (arrayValues == null) {
            return this;
        }

        if (!(percentileNode instanceof Value<?> percentileValue)) {
            return this;
        }

        if (percentileValue.getReturnType() != TypeEnum.INTEGER &&
                percentileValue.getReturnType() != TypeEnum.NUMBER) {
            return this;
        }

        double percentile = ((Number) percentileValue.getValue()).doubleValue();
        if (percentile < 0 || percentile > 100) {
            return this;
        }

        Value<?>[] elements = arrayValues.getValue();
        if (elements.length == 0) {
            return new NumberLiteral(0);
        }

        double[] values = new double[elements.length];
        for (int i = 0; i < elements.length; i++) {
            Node key = keySelector
                    .setVariableNameValue(ELEMENT_NAME, elements[i])
                    .evaluate();

            if (!(key instanceof Value<?> value)) {
                return this;
            }

            if (value.getReturnType() == TypeEnum.INTEGER) {
                values[i] = ((Number) value.getValue()).doubleValue();
            } else if (value.getReturnType() == TypeEnum.NUMBER) {
                values[i] = ((Number) value.getValue()).doubleValue();
            } else {
                return this;
            }
        }

        Arrays.sort(values);
        if (values.length == 1) {
            return new NumberLiteral(values[0]);
        }

        double position = percentile / 100.0 * (values.length - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) {
            return new NumberLiteral(values[lower]);
        }

        double fraction = position - lower;
        double result = values[lower] + fraction * (values[upper] - values[lower]);
        return new NumberLiteral(result);
    }

    @Override
    protected ArrayAggregation createArrayAggregation(Node array, Node keySelector, Node... parameters) {
        return new Percentile(array, keySelector, parameters[0]);
    }

    @Override
    public List<Node> getChildren() {
        return List.of(
                getParameter(0),
                getParameter(1),
                getParameter(2)
        );
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(
                new ArrayType(arrayElementType),
                TypeEnum.NUMBER,
                TypeEnum.NUMBER
        );
    }
}
