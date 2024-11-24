package at.sfischer.constraints.model.operators;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;

import java.util.List;
import java.util.Objects;

public abstract class BinaryOperator implements Operator {

    protected Node left;

    protected Node right;

    public BinaryOperator(Node left, Node right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean validate() {
        if(this.left == null || this.right == null){
            return false;
        }

        List<Type> operandTypes = operandTypes();
        if(operandTypes.size() != 2){
            return false;
        }

        Type leftType = operandTypes.get(0);
        Type rightType = operandTypes.get(1);
        return (this.left.getReturnType() == leftType || this.left.getReturnType() == TypeEnum.ANY)
                && (this.right.getReturnType() == rightType || this.left.getReturnType() == TypeEnum.ANY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinaryOperator that = (BinaryOperator) o;
        return Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(left: " + left + ", right: " + right + ")";
    }
}
