package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;

/** Converts a number to its standard Java string representation. */
public class NumberToString extends Function {

    private static final String FUNCTION_NAME = "string.numberToString";

    public NumberToString(Node value) {
        super(FUNCTION_NAME, value);
    }

    @Override
    public Node evaluate() {
        Number value = getNumberArgument(0);
        if (value != null) {
            return new StringLiteral(value.toString());
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new NumberToString(getParameter(0).setVariableValues(values));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.NUMBER);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.STRING;
    }
}
