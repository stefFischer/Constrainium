package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OneOfStringArray extends Function {

    private static final String FUNCTION_NAME = "string.OneOfArray";

    public OneOfStringArray(Node value, NumberLiteral numberOfOptions) {
        //noinspection unchecked
        this(value, new ArrayValues<ArrayValues<StringLiteral>>(new ArrayType(TypeEnum.STRING), new ArrayValues[numberOfOptions.getValue().intValue()]));
    }

    public OneOfStringArray(Node value, ArrayValues<ArrayValues<StringLiteral>> options) {
        super(FUNCTION_NAME, value, new NumberLiteral(options.getValue().length), options);
    }

    @Override
    public Node evaluate() {
        Node first = this.getParameter(0).evaluate();
        if(!(first instanceof ArrayValues<?>)){
            return this;
        }

        //noinspection unchecked
        ArrayValues<StringLiteral> firstValue = (ArrayValues<StringLiteral>) first;
        ArrayValues<?>[] options = this.getNestedArrayArgument(2);

        for (int i = 0; i < options.length; i++) {
            // There are still options.
            if (options[i] == null) {
                //noinspection unchecked
                ArrayValues<ArrayValues<StringLiteral>> optionParameter = (ArrayValues<ArrayValues<StringLiteral>>) getParameter(2);
                optionParameter.setValue(i, new ArrayValues<>(TypeEnum.STRING, firstValue.getValue()));
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
        return new OneOfStringArray(getParameter(0).setVariableValues(values), (ArrayValues<ArrayValues<StringLiteral>>) getParameter(2));
    }

    @Override
    public Node cloneNode() {
        //noinspection unchecked
        return new OneOfStringArray(getParameter(0).cloneNode(), (ArrayValues<ArrayValues<StringLiteral>>) getParameter(2).cloneNode());
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1), getParameter(2));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(TypeEnum.STRING), TypeEnum.NUMBER, new ArrayType(new ArrayType(TypeEnum.STRING)));
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Function function = (Function) o;
        return Objects.equals(getName(), function.getName())
                && Objects.equals(getParameter(0), function.getParameter(0))
                && Objects.equals(getParameter(1), function.getParameter(1));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getParameter(0), getParameter(1));
    }
}
