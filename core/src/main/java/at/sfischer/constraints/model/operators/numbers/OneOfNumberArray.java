package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OneOfNumberArray extends Function {

    private static final String FUNCTION_NAME = "number.OneOfArray";

    public OneOfNumberArray(Node value, NumberLiteral numberOfOptions) {
        //noinspection unchecked
        this(value, new ArrayValues<ArrayValues<NumberLiteral>>(new ArrayType(TypeEnum.NUMBER), new ArrayValues[numberOfOptions.getValue().intValue()]));
    }

    public OneOfNumberArray(Node value, ArrayValues<ArrayValues<NumberLiteral>> options) {
        super(FUNCTION_NAME, value, new NumberLiteral(options.getValue().length), options);
    }

    @Override
    public Node evaluate() {
        Node first = this.getParameter(0).evaluate();
        if(!(first instanceof ArrayValues<?>)){
            return this;
        }

        //noinspection unchecked
        ArrayValues<NumberLiteral> firstValue = (ArrayValues<NumberLiteral>) first;
        ArrayValues<?>[] options = this.getNestedArrayArgument(2);

        for (int i = 0; i < options.length; i++) {
            // There are still options.
            if (options[i] == null) {
                //noinspection unchecked
                ArrayValues<ArrayValues<NumberLiteral>> optionParameter = (ArrayValues<ArrayValues<NumberLiteral>>) getParameter(2);
                optionParameter.setValue(i, new ArrayValues<>(TypeEnum.NUMBER, firstValue.getValue()));
                return BooleanLiteral.TRUE;

            // The current value is already an option.
            } else if (Arrays.equals(options[i].getValue(), firstValue.getValue())) {
                return BooleanLiteral.TRUE;
            }
        }

        return BooleanLiteral.FALSE;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        //noinspection unchecked
        return new OneOfNumberArray(getParameter(0).setVariableValues(values), (ArrayValues<ArrayValues<NumberLiteral>>) getParameter(2));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1), getParameter(2));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(TypeEnum.NUMBER), TypeEnum.NUMBER, new ArrayType(new ArrayType(TypeEnum.NUMBER)));
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }
}