package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;

public class StringEquals extends Function {

    private static final String FUNCTION_NAME = "string.equals";

    public StringEquals(Node first, Node second) {
        super(FUNCTION_NAME, first, second);
    }

    @Override
    public Node evaluate() {
        String first = this.getStringArgument(0);
        String second = this.getStringArgument(1);

        if(first != null && second != null){
            boolean result = first.equals(second);
            return new BooleanLiteral(result);
        }

        if(getParameter(0).equals(getParameter(1))){
            return new BooleanLiteral(true);
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new StringEquals(getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
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
        return TypeEnum.BOOLEAN;
    }
}
