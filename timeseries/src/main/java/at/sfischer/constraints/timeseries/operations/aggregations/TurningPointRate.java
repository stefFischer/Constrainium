package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.List;
import java.util.Map;

public class TurningPointRate extends ArrayOperation {

    private static final String FUNCTION_NAME = "timeseries.turningPointRate";

    public TurningPointRate(Node array) {
        super(FUNCTION_NAME, array);
    }

    @Override
    public Node evaluate() {
        Node array = getParameter(0).evaluate();
        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return this;
        }

        Value<?>[] values = arrayValues.getValue();
        if (values.length < 3) {
            return new NumberLiteral(0);
        }

        int turningPoints = 0;
        for (int i = 1; i < values.length - 1; i++) {
            if (!(values[i - 1].getValue() instanceof Number previous) ||
                    !(values[i].getValue() instanceof Number current) ||
                    !(values[i + 1].getValue() instanceof Number next)) {
                return this;
            }

            double previousDifference = current.doubleValue() - previous.doubleValue();
            double nextDifference = next.doubleValue() - current.doubleValue();
            if (previousDifference * nextDifference < 0) {
                turningPoints++;
            }
        }

        return new NumberLiteral((double) turningPoints / (values.length - 2));
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
            context.error(this, "Turning-point rate requires an array of INTEGER or NUMBER values.");
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
        return new TurningPointRate(
                getParameter(0).setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new TurningPointRate(
                getParameter(0).cloneNode()
        );
    }
}