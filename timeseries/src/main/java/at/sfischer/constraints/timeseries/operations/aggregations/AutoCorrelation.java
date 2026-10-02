package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.List;
import java.util.Map;

public class AutoCorrelation extends ArrayOperation {

    private static final String FUNCTION_NAME = "timeseries.autoCorrelation";

    public AutoCorrelation(Node array, Node lag) {
        super(FUNCTION_NAME, array, lag);
    }

    @Override
    public Node evaluate() {
        Node array = getParameter(0).evaluate();
        Node lagNode = getParameter(1).evaluate();
        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return this;
        }

        if (!(lagNode instanceof Value<?> value) || !(value.getValue() instanceof Integer lag)) {
            return this;
        }

        if (lag < 0) {
            return this;
        }

        Value<?>[] values = arrayValues.getValue();
        int n = values.length;
        if (n == 0 || lag >= n) {
            return this;
        }

        double sum = 0;
        for (Value<?> valueAt : values) {
            if (!(valueAt.getValue() instanceof Number)) {
                return this;
            }

            sum += ((Number) valueAt.getValue()).doubleValue();
        }

        double mean = sum / n;
        double denominator = 0;
        for (Value<?> valueAt : values) {
            double x = ((Number) valueAt.getValue()).doubleValue();
            double deviation = x - mean;
            denominator += deviation * deviation;
        }

        if (denominator == 0) {
            return new NumberLiteral(0);
        }

        double numerator = 0;
        for (int i = lag; i < n; i++) {
            double current = ((Number) values[i].getValue()).doubleValue();
            double previous = ((Number) values[i - lag].getValue()).doubleValue();
            numerator += (current - mean) * (previous - mean);
        }

        return new NumberLiteral(numerator / denominator);
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        Node array = getParameter(0).evaluate();
        if (array.getReturnType() == TypeEnum.ANY) {
            return;
        }

        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return;
        }

        Type elementType = arrayValues.getElementType();
        if (elementType != TypeEnum.INTEGER && elementType != TypeEnum.NUMBER) {
            context.error(this, "Autocorrelation requires an array of INTEGER or NUMBER values.");
        }

        Node lag = getParameter(1);
        if (lag.getReturnType() != TypeEnum.ANY && lag.getReturnType() != TypeEnum.INTEGER) {
            context.error(this, "Autocorrelation lag must be INTEGER.");
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
    public List<Type> parameterTypes() {
        return List.of(
                new ArrayType(TypeEnum.NUMBER),
                TypeEnum.INTEGER
        );
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.NUMBER;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new AutoCorrelation(
                getParameter(0).setVariableValues(values),
                getParameter(1).setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new AutoCorrelation(
                getParameter(0).cloneNode(),
                getParameter(1).cloneNode()
        );
    }
}