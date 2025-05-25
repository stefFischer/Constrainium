package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;

public class ArrayIndex extends Function {

    private static final String FUNCTION_NAME = "arrays.index";

    public ArrayIndex(Node first, Node index) {
        super(FUNCTION_NAME, first, index);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> first = this.getArrayArgument(0);
        Number indexNumber = this.getNumberArgument(1);
        if(first == null || indexNumber == null) {
            return this;
        }

        int index = indexNumber.intValue();
        if(index < 0 || index >= first.getValue().length){
            // TODO Maybe there should be an index out of bound case where we return that the constraint is invalid. Otherwise it will just count as inapplicable, even though this should be a counter example.
            return this;
        }

        return first.getValue()[index];
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new ArrayIndex(getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(TypeEnum.ANY), TypeEnum.NUMBER);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.ANY;
    }
}
