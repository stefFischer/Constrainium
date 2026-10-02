package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.*;

import java.util.List;
import java.util.Map;

public class Min extends NumberFunction {

    private static final String FUNCTION_NAME = "min";

    public Min(Node left, Node right) {
        super(FUNCTION_NAME, left, right);
    }

    @Override
    public Node evaluate() {
        Number left = this.getNumberArgument(0);
        Number right = this.getNumberArgument(1);
        if(left != null && right != null){
            double result = Math.min(left.doubleValue(), right.doubleValue());
            return new NumberLiteral(result);
        }

        return this;
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new Min(getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.NUMBER, TypeEnum.NUMBER);
    }
}