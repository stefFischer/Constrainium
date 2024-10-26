package at.sfischer.constraints.model.operators;

import at.sfischer.constraints.model.Node;

import java.util.Objects;

public abstract class UnaryOperator implements Operator {

    protected Node operand;

    public UnaryOperator(Node operand) {
        this.operand = operand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnaryOperator that = (UnaryOperator) o;
        return Objects.equals(operand, that.operand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operand);
    }
}
