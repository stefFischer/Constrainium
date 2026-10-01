package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.*;

import java.util.List;
import java.util.Map;

public class Max extends NumberFunction {

    private static final String FUNCTION_NAME = "max";

    public Max(Node left, Node right) {
        super(FUNCTION_NAME, left, right);
    }

    @Override
    public Node evaluate() {
        Number left = this.getNumberArgument(0);
        Number right = this.getNumberArgument(1);
        if(left != null && right != null){
            double result = Math.max(left.doubleValue(), right.doubleValue());
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
        return new Max(getParameter(0), getParameter(1));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.NUMBER, TypeEnum.NUMBER);
    }
}