package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayOperation;

import java.util.List;
import java.util.Map;

public class FourierMagnitude extends ArrayOperation {

    private static final String FUNCTION_NAME = "timeseries.fourierMagnitude";

    public FourierMagnitude(Node array, Node frequency) {
        super(FUNCTION_NAME, array, frequency);
    }

    @Override
    public Node evaluate() {
        Node array = getParameter(0).evaluate();
        Node frequencyNode = getParameter(1).evaluate();
        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return this;
        }

        if (!(frequencyNode instanceof Value<?> value) || !(value.getValue() instanceof Number number)) {
            return this;
        }

        int frequency = number.intValue();
        Value<?>[] values = arrayValues.getValue();
        int n = values.length;
        if (frequency < 0 || frequency >= n) {
            return this;
        }

        double real = 0;
        double imaginary = 0;
        for (int t = 0; t < n; t++) {
            if (!(values[t].getValue() instanceof Number signalValue)) {
                return this;
            }

            double x = signalValue.doubleValue();
            double angle = 2.0 * Math.PI * frequency * t / n;
            real += x * Math.cos(angle);
            imaginary -= x * Math.sin(angle);
        }

        double magnitude = Math.sqrt(real * real + imaginary * imaginary);
        return new NumberLiteral(magnitude);
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(TypeEnum.NUMBER), TypeEnum.NUMBER);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.NUMBER;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new FourierMagnitude(getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
    }

    @Override
    public Node cloneNode() {
        return new FourierMagnitude(getParameter(0).cloneNode(), getParameter(1).cloneNode());
    }
}
