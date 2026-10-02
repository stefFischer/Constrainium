package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.validation.ValidationContext;
import at.sfischer.constraints.timeseries.operations.helper.Welch;
import at.sfischer.constraints.timeseries.operations.helper.WelchSpectrum;

import java.util.List;
import java.util.Map;

public class DominantFrequency extends ArrayOperation {

    private static final String FUNCTION_NAME = "timeseries.dominantFrequency";

    public DominantFrequency(
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
                !(windowSizeValue.getValue() instanceof Number windowSize)) {
            return this;
        }

        if (!(samplingRateNode instanceof Value<?> samplingRateValue) ||
                !(samplingRateValue.getValue() instanceof Number samplingRate)) {
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
            WelchSpectrum spectrum = Welch.calculate(signal, windowSize.intValue(), samplingRate.doubleValue());
            double[] frequencies = spectrum.getFrequencies();
            double[] powers = spectrum.getPowers();
            if (powers.length == 0) {
                return this;
            }

            int maximumIndex = 0;
            for (int i = 1; i < powers.length; i++) {
                if (powers[i] > powers[maximumIndex]) {
                    maximumIndex = i;
                }
            }

            return new NumberLiteral(frequencies[maximumIndex]);
        } catch (IllegalArgumentException e) {
            return this;
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
        return new DominantFrequency(
                getParameter(0).setVariableValues(values),
                getParameter(1).setVariableValues(values),
                getParameter(2).setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new DominantFrequency(
                getParameter(0).cloneNode(),
                getParameter(1).cloneNode(),
                getParameter(2).cloneNode()
        );
    }
}