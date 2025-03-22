package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OneOfString extends Function {

    private static final String FUNCTION_NAME = "string.OneOf";

    public OneOfString(Node value, NumberLiteral numberOfOptions) {
        this(value, new ArrayValues<>(TypeEnum.STRING, new StringLiteral[numberOfOptions.getValue().intValue()]));
    }

    public OneOfString(Node value, ArrayValues<StringLiteral> options) {
        super(FUNCTION_NAME, value, new NumberLiteral(options.getValue().length), options);
    }

    @Override
    public Node evaluate() {
        String first = this.getStringArgument(0);
        String[] options = this.getStringArrayArgument(2);

        if(first != null & options != null) {
            for (int i = 0; i < options.length; i++) {
                // There are still options.
                if (options[i] == null) {
                    //noinspection unchecked
                    ArrayValues<StringLiteral> optionParameter = (ArrayValues<StringLiteral>) getParameter(2);
                    optionParameter.setValue(i, new StringLiteral(first));
                    return BooleanLiteral.TRUE;

                // The current value is already an option.
                } else if (options[i].equals(first)) {
                    return BooleanLiteral.TRUE;
                }
            }

            return BooleanLiteral.FALSE;
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        //noinspection unchecked
        return new OneOfString(getParameter(0).setVariableValues(values), (ArrayValues<StringLiteral>) getParameter(2));
    }

    @Override
    public Node cloneNode() {
        //noinspection unchecked
        return new OneOfString(getParameter(0).cloneNode(), (ArrayValues<StringLiteral>) getParameter(2).cloneNode());
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1), getParameter(2));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.STRING, TypeEnum.NUMBER, new ArrayType(TypeEnum.STRING));
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
