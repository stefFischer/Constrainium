package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;

public class ArrayEquals extends Function {

    private static final String FUNCTION_NAME = "arrays.equals";

    public ArrayEquals(Node first, Node second) {
        super(FUNCTION_NAME, first, second);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> array1 = getArrayArgument(0);
        ArrayValues<?> array2 = getArrayArgument(1);

        if(array1 != null && array2 != null){
            boolean result = array1.equals(array2);
            return BooleanLiteral.getBooleanLiteral(result);
        }

        if(getParameter(0).equals(getParameter(1))){
            return BooleanLiteral.TRUE;
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new ArrayEquals(getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(TypeEnum.ANY), new ArrayType(TypeEnum.ANY));
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }
}
