package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.List;
import java.util.Map;

public class LinearTrend extends ArrayOperation {

    private static final String FUNCTION_NAME = "timeseries.linearTrend";

    public LinearTrend(Node array) {
        super(FUNCTION_NAME, array);
    }

    @Override
    public Node evaluate() {
        Node array = getParameter(0).evaluate();
        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return this;
        }

        Value<?>[] values = arrayValues.getValue();
        if (values.length < 2) {
            return new NumberLiteral(0);
        }

        int n = values.length;
        double sumT = 0;
        double sumX = 0;
        double sumTX = 0;
        double sumT2 = 0;
        for (int i = 0; i < n; i++) {
            if (!(values[i].getValue() instanceof Number number)) {
                return this;
            }

            double t = i + 1;
            double x = number.doubleValue();
            sumT += t;
            sumX += x;
            sumTX += t * x;
            sumT2 += t * t;
        }

        double numerator = n * sumTX - sumT * sumX;
        double denominator = n * sumT2 - sumT * sumT;
        if (denominator == 0) {
            return new NumberLiteral(0);
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
            context.error(this, "Trend operation requires an array of INTEGER or NUMBER values.");
        }
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(TypeEnum.NUMBER));
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.NUMBER;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new LinearTrend(getParameter(0).setVariableValues(values));
    }

    @Override
    public Node cloneNode() {
        return new LinearTrend(getParameter(0).cloneNode());
    }
}
