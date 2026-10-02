package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.*;

import java.util.List;
import java.util.Map;

public class Cos extends NumberFunction {

    private static final String FUNCTION_NAME = "cos";

    public Cos(Node arg) {
        super(FUNCTION_NAME, arg);
    }

    @Override
    public Node evaluate() {
        Number arg = this.getNumberArgument(0);
        if(arg != null){
            double result = Math.cos(arg.doubleValue());
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
        return new Cos(getParameter(0).setVariableValues(values));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.NUMBER);
    }
}