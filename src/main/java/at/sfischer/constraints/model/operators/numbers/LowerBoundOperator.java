package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LowerBoundOperator extends Function {

    private static final String FUNCTION_NAME = "number.LowerBound";

    public LowerBoundOperator(Node value) {
        this(
                value,
                new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                        new NumberLiteral(-1),
                        new NumberLiteral(0),
                        new NumberLiteral(1),
                        new NumberLiteral(2)
                }),
                new NumberLiteral(3),
                new NumberLiteral(5)
        );
    }

    public LowerBoundOperator(Node value, ArrayValues<NumberLiteral> bounds, NumberLiteral minValuesAtBound, NumberLiteral minValues) {
        this(value, bounds, minValuesAtBound, new NumberLiteral(0), minValues, new NumberLiteral(0));
    }

    private LowerBoundOperator(Node value, ArrayValues<NumberLiteral> bounds, NumberLiteral minValuesAtBound, NumberLiteral valuesAtBoundCounter, NumberLiteral minValues, NumberLiteral valuesCounter) {
        super(FUNCTION_NAME, value, bounds, minValuesAtBound, valuesAtBoundCounter, minValues, valuesCounter);
        Arrays.sort(bounds.getValue(), (o1, o2) -> {
            double diff = o1.getValue().doubleValue() - o2.getValue().doubleValue();
            if(diff < 0) {
                return -1;
            } else if (diff > 0) {
                return 1;
            }

            return 0;
        });
    }

    @Override
    public Node evaluate() {
        Number value = this.getNumberArgument(0);
        Number[] bounds = this.getNumberArrayArgument(1);
        Number minValuesAtBound = this.getNumberArgument(2);
        Number valuesAtBoundCounter = this.getNumberArgument(3);
        Number minValues = this.getNumberArgument(4);
        Number valuesCounter = this.getNumberArgument(5);
        if(value == null || bounds == null || minValuesAtBound == null || valuesAtBoundCounter == null) {
            return this;
        }

        if(bounds.length == 0){
            // There are no more bounds to be checked, because we found a value lower than all bounds.
            return new BooleanLiteral(false);
        }

        valuesCounter = valuesCounter.intValue()  + 1;
        NumberLiteral valuesCounterParameter = (NumberLiteral) getParameter(5);
        valuesCounterParameter.setValue(valuesCounter);

        Number currentBound = bounds[bounds.length - 1];
        if(currentBound.doubleValue() == value.doubleValue()){
            valuesAtBoundCounter = valuesAtBoundCounter.intValue() + 1;
            NumberLiteral valuesAtBoundCounterParameter = (NumberLiteral) getParameter(3);
            valuesAtBoundCounterParameter.setValue(valuesAtBoundCounter);
        }

        if(currentBound.doubleValue() <= value.doubleValue()){
            if(valuesAtBoundCounter.intValue() >= minValuesAtBound.intValue() && valuesCounter.intValue() >= minValues.intValue()){
                return new BooleanLiteral(true);
            }

            return MoreStatisticalEvidenceNeeded.INSTANCE;
        }

        int newIndex = bounds.length - 2;
        while(newIndex >= 0){
            currentBound = bounds[newIndex];
            if(currentBound.doubleValue() == value.doubleValue()){
                break;
            }

            newIndex--;
        }

        if(newIndex < 0){
            // Update bounds to empty array, because we found a value lower than all of them.
            //noinspection unchecked
            ArrayValues<NumberLiteral> boundsParameter = (ArrayValues<NumberLiteral>) getParameter(1);
            boundsParameter.setValue(new NumberLiteral[]{});

            // Update valuesAtBoundCounter.
            NumberLiteral valuesAtBoundCounterParameter = (NumberLiteral) getParameter(3);
            valuesAtBoundCounterParameter.setValue(0);
            return new BooleanLiteral(false);
        }

        NumberLiteral[] newBounds = new NumberLiteral[newIndex + 1];
        for (int i = 0; i < newBounds.length; i++) {
            newBounds[i] = new NumberLiteral(bounds[i]);
        }
        // Update bounds to empty array with the bounds that still could apply.
        //noinspection unchecked
        ArrayValues<NumberLiteral> boundsParameter = (ArrayValues<NumberLiteral>) getParameter(1);
        boundsParameter.setValue(newBounds);

        // Update valuesAtBoundCounter.
        NumberLiteral valuesAtBoundCounterParameter = (NumberLiteral) getParameter(3);
        valuesAtBoundCounterParameter.setValue(1);
        valuesAtBoundCounter = 1;
        if(valuesAtBoundCounter.intValue() >= minValuesAtBound.intValue() && valuesCounter.intValue() >= minValues.intValue()){
            return new BooleanLiteral(true);
        }

        return MoreStatisticalEvidenceNeeded.INSTANCE;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        //noinspection unchecked
        return new LowerBoundOperator(getParameter(0).setVariableValues(values), (ArrayValues<NumberLiteral>) getParameter(1), ((NumberLiteral) getParameter(2)), ((NumberLiteral) getParameter(3)), ((NumberLiteral) getParameter(4)), ((NumberLiteral) getParameter(5)));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1), getParameter(2), getParameter(3), getParameter(4), getParameter(5));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.NUMBER, new ArrayType(TypeEnum.NUMBER), TypeEnum.NUMBER, TypeEnum.NUMBER, TypeEnum.NUMBER, TypeEnum.NUMBER);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }
}
