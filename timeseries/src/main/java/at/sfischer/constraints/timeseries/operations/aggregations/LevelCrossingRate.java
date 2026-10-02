package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.List;
import java.util.Map;

public class LevelCrossingRate extends ArrayOperation {

    private static final String FUNCTION_NAME = "timeseries.levelCrossingRate";

    public LevelCrossingRate(
            Node array,
            Node level
    ) {
        super(FUNCTION_NAME, array, level);
    }

    @Override
    public Node evaluate() {
        Node array = getParameter(0).evaluate();
        Node levelNode = getParameter(1).evaluate();
        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return this;
        }

        if (!(levelNode instanceof Value<?> levelValue) ||
                !(levelValue.getValue() instanceof Number levelNumber)) {
            return this;
        }

        double level = levelNumber.doubleValue();
        Value<?>[] values = arrayValues.getValue();
        if (values.length < 2) {
            return new NumberLiteral(0);
        }

        int crossings = 0;
        for (int i = 1; i < values.length; i++) {
            if (!(values[i - 1].getValue() instanceof Number previous) ||
                    !(values[i].getValue() instanceof Number current)) {
                return this;
            }

            double previousDeviation = previous.doubleValue() - level;
            double currentDeviation = current.doubleValue() - level;
            if (previousDeviation * currentDeviation < 0) {
                crossings++;
            }
        }

        return new NumberLiteral((double) crossings / (values.length - 1));
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
            context.error(this, "Level crossing rate requires an array of INTEGER or NUMBER values.");
        }

        Node level = getParameter(1);
        Type levelType = level.getReturnType();
        if (levelType != TypeEnum.ANY &&
                levelType != TypeEnum.INTEGER &&
                levelType != TypeEnum.NUMBER) {
            context.error(this, "Level crossing rate level must be INTEGER or NUMBER.");
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
                TypeEnum.NUMBER
        );
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.NUMBER;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new LevelCrossingRate(
                getParameter(0).setVariableValues(values),
                getParameter(1).setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new LevelCrossingRate(
                getParameter(0).cloneNode(),
                getParameter(1).cloneNode()
        );
    }
}