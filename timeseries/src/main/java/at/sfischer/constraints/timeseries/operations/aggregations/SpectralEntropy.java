package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.validation.ValidationContext;
import at.sfischer.constraints.timeseries.operations.helper.Welch;
import at.sfischer.constraints.timeseries.operations.helper.WelchSpectrum;

import java.util.List;
import java.util.Map;

public class SpectralEntropy extends ArrayOperation {

    private static final String FUNCTION_NAME = "timeseries.spectralEntropy";

    public SpectralEntropy(
            Node array,
            Node windowSize,
            Node samplingRate
    ) {
        super(FUNCTION_NAME, array, windowSize, samplingRate);
    }

    @Override
    public Node evaluate() {
        Node array = getParameter(0).evaluate();
        Node windowSizeNode = getParameter(1).evaluate();
        Node samplingRateNode = getParameter(2).evaluate();
        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return this;
        }

        if (!(windowSizeNode instanceof Value<?> windowSizeValue) ||
                !(windowSizeValue.getValue() instanceof Integer windowSize)) {
            return this;
        }

        if (!(samplingRateNode instanceof Value<?> samplingRateValue) ||
                !(samplingRateValue.getValue() instanceof Number samplingRateNumber)) {
            return this;
        }

        double samplingRate = samplingRateNumber.doubleValue();
        if (windowSize < 2 || samplingRate <= 0) {
            return this;
        }

        Value<?>[] values = arrayValues.getValue();
        double[] signal = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            if (!(values[i].getValue() instanceof Number number)) {
                return this;
            }

            signal[i] = number.doubleValue();
        }

        try {
            WelchSpectrum spectrum = Welch.calculate(signal, windowSize, samplingRate);
            double[] powers = spectrum.getPowers();
            if (powers.length == 0) {
                return this;
            }

            double totalPower = 0;
            for (double power : powers) {
                totalPower += power;
            }
            if (totalPower == 0) {
                return new NumberLiteral(0);
            }

            double entropy = 0;
            for (double power : powers) {
                if (power <= 0) {
                    continue;
                }

                double probability = power / totalPower;
                entropy -= probability * Math.log(probability);
            }

            return new NumberLiteral(entropy);
        } catch (IllegalArgumentException e) {
            return this;
        }
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
            context.error(this, "Spectral entropy requires an array of INTEGER or NUMBER values.");
        }

        Node windowSize = getParameter(1);
        if (windowSize.getReturnType() != TypeEnum.ANY && windowSize.getReturnType() != TypeEnum.INTEGER) {
            context.error(this, "Spectral entropy window size must be INTEGER.");
        }

        Node samplingRate = getParameter(2);
        Type samplingRateType = samplingRate.getReturnType();
        if (samplingRateType != TypeEnum.ANY &&
                samplingRateType != TypeEnum.INTEGER &&
                samplingRateType != TypeEnum.NUMBER) {

            context.error(this, "Spectral entropy sampling rate must be INTEGER or NUMBER.");
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
                TypeEnum.INTEGER,
                TypeEnum.NUMBER
        );
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.NUMBER;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new SpectralEntropy(
                getParameter(0).setVariableValues(values),
                getParameter(1).setVariableValues(values),
                getParameter(2).setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new SpectralEntropy(
                getParameter(0).cloneNode(),
                getParameter(1).cloneNode(),
                getParameter(2).cloneNode()
        );
    }
}