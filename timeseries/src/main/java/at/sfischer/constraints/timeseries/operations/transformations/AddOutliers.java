package at.sfischer.constraints.timeseries.operations.transformations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AddOutliers extends ArrayOperation {

    private static final String FUNCTION_NAME = "timeseries.addOutliers";
    private final Random random;

    public AddOutliers(Node array, Node k, Node alpha) {
        this(array, k, alpha, new Random());
    }

    public AddOutliers(
            Node array,
            Node k,
            Node alpha,
            Random random
    ) {
        super(FUNCTION_NAME, array, k, alpha);
        this.random = random;
    }

    @Override
    public Node evaluate() {
        Node array = getParameter(0).evaluate();
        Node kNode = getParameter(1).evaluate();
        Node alphaNode = getParameter(2).evaluate();
        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return this;
        }

        if (!(kNode instanceof Value<?> kValue) ||
                !(kValue.getValue() instanceof Number kNum)) {
            return this;
        }

        if (!(alphaNode instanceof Value<?> alphaValue) ||
                !(alphaValue.getValue() instanceof Number alphaNumber)) {
            return this;
        }

        int k = kNum.intValue();
        if (k < 0) {
            return this;
        }

        Value<?>[] values = arrayValues.getValue();
        if (k > values.length) {
            return this;
        }

        if (values.length == 0 || k == 0) {
            return arrayValues;
        }

        double[] numbers = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            if (!(values[i].getValue() instanceof Number number)) {
                return this;
            }

            numbers[i] = number.doubleValue();
        }

        double mean = 0;
        for (double number : numbers) {
            mean += number;
        }
        mean /= numbers.length;

        double variance = 0;
        for (double number : numbers) {
            double deviation = number - mean;
            variance += deviation * deviation;
        }
        variance /= numbers.length;

        double standardDeviation = Math.sqrt(variance);
        double offset = alphaNumber.doubleValue() * standardDeviation;
        List<Integer> indices = new ArrayList<>(values.length);
        for (int i = 0; i < values.length; i++) {
            indices.add(i);
        }

        Collections.shuffle(indices, random);
        boolean[] outlier = new boolean[values.length];
        for (int i = 0; i < k; i++) {
            outlier[indices.get(i)] = true;
        }

        Value<?>[] result = new Value<?>[values.length];
        for (int i = 0; i < values.length; i++) {
            if (outlier[i]) {
                result[i] = new NumberLiteral(
                        numbers[i] + offset
                );
            } else {
                result[i] = values[i];
            }
        }

        return new ArrayValues<>(
                TypeEnum.NUMBER,
                result
        );
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
            context.error(this, "Add outliers requires an array of INTEGER or NUMBER values.");
        }

        Node k = getParameter(1);
        if (k.getReturnType() != TypeEnum.INTEGER && k.getReturnType() != TypeEnum.NUMBER) {
            context.error(this, "Add outliers k must be INTEGER or NUMBER.");
        }

        Node alpha = getParameter(2);
        Type alphaType = alpha.getReturnType();
        if (alphaType != TypeEnum.ANY &&
                alphaType != TypeEnum.INTEGER &&
                alphaType != TypeEnum.NUMBER) {
            context.error(this, "Add outliers alpha must be INTEGER or NUMBER.");
        }
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
                new ArrayType(TypeEnum.NUMBER),
                TypeEnum.NUMBER,
                TypeEnum.NUMBER
        );
    }

    @Override
    public Type getReturnType() {
        return new ArrayType(TypeEnum.NUMBER);
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new AddOutliers(
                getParameter(0).setVariableValues(values),
                getParameter(1).setVariableValues(values),
                getParameter(2).setVariableValues(values),
                random
        );
    }

    @Override
    public Node cloneNode() {
        return new AddOutliers(
                getParameter(0).cloneNode(),
                getParameter(1).cloneNode(),
                getParameter(2).cloneNode(),
                random
        );
    }
}