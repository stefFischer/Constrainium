package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;

public class ArrayLength extends Function {

    private static final String FUNCTION_NAME = "arrays.length";

    public ArrayLength(Node first) {
        super(FUNCTION_NAME, first);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> first = this.getArrayArgument(0);
        if(first != null){
            return new NumberLiteral(first.getValue().length);
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new ArrayLength(getParameter(0).setVariableValues(values));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(TypeEnum.ANY));
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.NUMBER;
    }
}
