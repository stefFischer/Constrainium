package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.*;

import java.util.List;
import java.util.Map;

public class Sin extends NumberFunction {

    private static final String FUNCTION_NAME = "sin";

    public Sin(Node arg) {
        super(FUNCTION_NAME, arg);
    }

    @Override
    public Node evaluate() {
        Number arg = this.getNumberArgument(0);
        if(arg != null){
            double result = Math.sin(arg.doubleValue());
            return new NumberLiteral(result);
        }

        return this;
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0));
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new Sin(getParameter(0));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.NUMBER);
    }
}