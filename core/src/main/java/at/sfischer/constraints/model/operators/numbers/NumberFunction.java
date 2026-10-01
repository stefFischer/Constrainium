package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;

public abstract class NumberFunction extends Function {

    public NumberFunction(String name, List<Node> parameters) {
        super(name, parameters);
    }

    public NumberFunction(String name, Node... parameters) {
        super(name, parameters);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.NUMBER;
    }
}
