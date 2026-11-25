package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;

/** Concatenates two strings in argument order, without a separator. */
public class StringConcat extends Function {

    private static final String FUNCTION_NAME = "string.concat";

    public StringConcat(Node first, Node second) {
        super(FUNCTION_NAME, first, second);
    }

    @Override
    public Node evaluate() {
        String first = getStringArgument(0);
        String second = getStringArgument(1);

        if (first != null && second != null) {
            return new StringLiteral(first + second);
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new StringConcat(getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.STRING, TypeEnum.STRING);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.STRING;
    }
}
