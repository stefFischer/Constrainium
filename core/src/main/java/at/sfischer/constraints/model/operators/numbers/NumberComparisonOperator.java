package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;

public abstract class NumberComparisonOperator extends BinaryNumberOperator {

    public NumberComparisonOperator(Node left, Node right) {
        super(left, right);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }
}
