package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;

public class StringLength extends Function {

    private static final String FUNCTION_NAME = "string.length";

    public StringLength(Node first) {
        super(FUNCTION_NAME, first);
    }

    @Override
    public Node evaluate() {
        String first = this.getStringArgument(0);
        if(first != null){
            return new NumberLiteral(first.length());
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Literal<?>> values) {
        return new StringLength(getParameter(0).setVariableValues(values));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.STRING);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.NUMBER;
    }
}
