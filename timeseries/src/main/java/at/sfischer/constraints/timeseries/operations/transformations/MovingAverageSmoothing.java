package at.sfischer.constraints.timeseries.operations.transformations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MovingAverageSmoothing extends ArrayOperation {

    private static final String FUNCTION_NAME = "timeseries.movingAverageSmoothing";

    public MovingAverageSmoothing(Node array, Node windowSize) {
        super(FUNCTION_NAME, array, windowSize);
    }

    @Override
    public Node evaluate() {
        Node array = getParameter(0).evaluate();
        Node windowSizeNode = getParameter(1).evaluate();
        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return this;
        }

        if (!(windowSizeNode instanceof Value<?> value) || !(value.getValue() instanceof Number number)) {
            return this;
        }

        int windowSize = number.intValue();
        if (windowSize <= 0) {
            return this;
        }

        Value<?>[] values = arrayValues.getValue();
        List<Value<?>> result = new ArrayList<>(values.length);
        for (int i = 0; i < values.length; i++) {
            int start = Math.max(0, i - windowSize + 1);
            double sum = 0;
            for (int j = start; j <= i; j++) {
                if (!(values[j].getValue() instanceof Number valueNumber)) {
                    return this;
                }

                sum += valueNumber.doubleValue();
            }

            double average = sum / (i - start + 1);
            result.add(new NumberLiteral(average));
        }

        return new ArrayValues<>(TypeEnum.NUMBER, result.toArray(new Value<?>[0]));
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        Node array = getParameter(0).evaluate();
        if (array.getReturnType() != TypeEnum.ANY && array instanceof ArrayValues<?> arrayValues) {
            Type elementType = arrayValues.getElementType();
            if (elementType != TypeEnum.INTEGER && elementType != TypeEnum.NUMBER) {
                context.error(this, "Moving average smoothing requires an array of INTEGER or NUMBER values.");
            }
        }

        Node windowSize = getParameter(1);
        Type windowSizeType = windowSize.getReturnType();
        if (windowSizeType != TypeEnum.ANY && windowSizeType != TypeEnum.INTEGER) {
            context.error(this, "Moving average smoothing window size must be INTEGER.");
        }
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(TypeEnum.NUMBER), TypeEnum.INTEGER);
    }

    @Override
    public Type getReturnType() {
        return new ArrayType(TypeEnum.NUMBER);
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new MovingAverageSmoothing(getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
    }

    @Override
    public Node cloneNode() {
        return new MovingAverageSmoothing(getParameter(0).cloneNode(), getParameter(1).cloneNode());
    }
}