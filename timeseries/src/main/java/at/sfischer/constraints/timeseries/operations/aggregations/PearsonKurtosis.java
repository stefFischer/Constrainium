package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.List;
import java.util.Map;

public class PearsonKurtosis extends ArrayOperation {

    private static final String FUNCTION_NAME = "timeseries.pearsonKurtosis";

    public PearsonKurtosis(Node array) {
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

        double sum = 0;
        for (Value<?> value : values) {
            if (!(value.getValue() instanceof Number number)) {
                return this;
            }

            sum += number.doubleValue();
        }

        double mean = sum / values.length;
        double secondMoment = 0;
        double fourthMoment = 0;
        for (Value<?> value : values) {
            double deviation = ((Number) value.getValue()).doubleValue() - mean;
            double squaredDeviation = deviation * deviation;
            secondMoment += squaredDeviation;
            fourthMoment += squaredDeviation * squaredDeviation;
        }

        secondMoment /= values.length;
        fourthMoment /= values.length;
        if (secondMoment == 0) {
            return new NumberLiteral(0);
        }

        double kurtosis = fourthMoment / (secondMoment * secondMoment);
        return new NumberLiteral(kurtosis);
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
            context.error(this, "Kurtosis requires an array of INTEGER or NUMBER values.");
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
        return new PearsonKurtosis(
                getParameter(0).setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new PearsonKurtosis(
                getParameter(0).cloneNode()
        );
    }
}