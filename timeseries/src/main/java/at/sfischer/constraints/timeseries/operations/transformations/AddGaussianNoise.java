package at.sfischer.constraints.timeseries.operations.transformations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class AddGaussianNoise extends ArrayOperation {

    private static final String FUNCTION_NAME = "timeseries.addGaussianNoise";

    public AddGaussianNoise(Node array, Node standardDeviation) {
        super(FUNCTION_NAME, array, standardDeviation);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node standardDeviationNode = getParameter(1).evaluate();
        if (arrayValues == null) {
            return this;
        }

        if (!(standardDeviationNode instanceof Value<?> value)) {
            return this;
        }

        if (value.getReturnType() != TypeEnum.INTEGER && value.getReturnType() != TypeEnum.NUMBER) {
            return this;
        }

        double standardDeviation = ((Number) value.getValue()).doubleValue();
        if (standardDeviation < 0) {
            return this;
        }

        List<Value<?>> resultValues = new ArrayList<>();
        for (Value<?> element : arrayValues.getValue()) {
            if (element.getReturnType() != TypeEnum.INTEGER && element.getReturnType() != TypeEnum.NUMBER) {
                return this;
            }

            double number = ((Number) element.getValue()).doubleValue();
            double noise = ThreadLocalRandom.current().nextGaussian(0, standardDeviation);
            resultValues.add(new NumberLiteral(number + noise));
        }

        return new ArrayValues<>(TypeEnum.NUMBER, resultValues.toArray(new Value[0]));
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
            context.error(this, "Add noise operation requires an array of INTEGER or NUMBER values.");
        }

        Node standardDeviation = getParameter(1);
        Type standardDeviationType = standardDeviation.getReturnType();
        if (standardDeviationType != TypeEnum.ANY &&
                standardDeviationType != TypeEnum.INTEGER &&
                standardDeviationType != TypeEnum.NUMBER) {

            context.error(this, "Add noise operation standard deviation must be INTEGER or NUMBER.");
        }
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
        return new ArrayType(TypeEnum.NUMBER);
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new AddGaussianNoise(getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
    }

    @Override
    public Node cloneNode() {
        return new AddGaussianNoise(getParameter(0).cloneNode(), getParameter(1).cloneNode());
    }
}