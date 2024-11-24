package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;

public abstract class ArithmeticOperator extends BinaryNumberOperator {

    public ArithmeticOperator(Node left, Node right) {
        super(left, right);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.NUMBER;
    }
}
