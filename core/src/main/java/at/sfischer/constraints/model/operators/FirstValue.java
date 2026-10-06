package at.sfischer.constraints.model.operators;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.Value;
import at.sfischer.constraints.model.Variable;

import java.util.List;
import java.util.Map;

public class FirstValue extends Function {

    private static final String FUNCTION_NAME = "firstValue";

    public FirstValue(Node first, Node second) {
        super(FUNCTION_NAME, first, second);
    }

    @Override
    public Node evaluate() {
        for (Node parameter : parameters) {
            Node evaluated = parameter.evaluate();
            if (evaluated instanceof Value<?>) {
                return evaluated;
            }
        }

        return this;
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.ANY, TypeEnum.ANY);
    }

    @Override
    public Type getReturnType() {
        Type firstType = getParameter(0).getReturnType();
        Type secondType = getParameter(1).getReturnType();
        if (firstType.equals(secondType)) {
            return firstType;
        }

        return TypeEnum.ANY;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new FirstValue(
                getParameter(0).setVariableValues(values),
                getParameter(1).setVariableValues(values)
        );
    }
}
