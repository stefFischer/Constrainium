package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;

public class SubString extends Function {

    private static final String FUNCTION_NAME = "string.subString";

    public SubString(Node first, Node second) {
        super(FUNCTION_NAME, first, second);
    }

    @Override
    public Node evaluate() {
        String first = this.getStringArgument(0);
        String second = this.getStringArgument(1);

        if(first == null || second == null){
            return this;
        }

        if(first.isEmpty() || first.equals(second)){
            return BooleanLiteral.FALSE;
        }

        if(!second.isEmpty()){
            boolean result = first.contains(second);
            return BooleanLiteral.getBooleanLiteral(result);
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new SubString(getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
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
