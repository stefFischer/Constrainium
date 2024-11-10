package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SuperSet extends Function {

    private static final String FUNCTION_NAME = "arrays.superSet";

    private final Type arrayElementType;

    public SuperSet(Node a, Node b) {
        this(TypeEnum.ANY, a, b);
    }

    public SuperSet(Type arrayElementType, Node a, Node b) {
        super(FUNCTION_NAME, a, b);
        this.arrayElementType = arrayElementType;
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> a = getArrayArgument(0);
        ArrayValues<?> b = getArrayArgument(1);
        Set<?> setA = Set.of(a.getValue());
        Set<?> setB = Set.of(b.getValue());
        //noinspection SuspiciousMethodCalls
        return new BooleanLiteral(setA.containsAll(setB));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new SuperSet(arrayElementType, getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(arrayElementType), new ArrayType(arrayElementType));
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }
}
