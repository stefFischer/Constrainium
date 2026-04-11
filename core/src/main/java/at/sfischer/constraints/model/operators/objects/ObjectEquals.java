package at.sfischer.constraints.model.operators.objects;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;

public class ObjectEquals extends Function {

    private static final String FUNCTION_NAME = "objects.equals";

    public ObjectEquals(Node first, Node second) {
        super(FUNCTION_NAME, first, second);
    }

    @Override
    public Node evaluate() {
        ComplexValue object1 = getComplexValue(0);
        ComplexValue object2 = getComplexValue(1);

        if(object1 != null && object2 != null){
            boolean result = object1.equals(object2);
            return BooleanLiteral.getBooleanLiteral(result);
        }

        if(getParameter(0).equals(getParameter(1))){
            return BooleanLiteral.TRUE;
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new ObjectEquals(getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.COMPLEXTYPE, TypeEnum.COMPLEXTYPE);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }
}
