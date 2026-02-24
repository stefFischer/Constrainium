package at.sfischer.constraints.model.operators;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.List;
import java.util.Objects;

public abstract class BinaryOperator implements Operator {

    protected Node left;

    protected Node right;

    public BinaryOperator(Node left, Node right) {
        this.left = left;
        this.right = right;
    }

    public Node getOperand1() {
        return left;
    }

    public Node getOperand2() {
        return right;
    }

    @Override
    public void validate(ValidationContext context) {
        if(this.left == null || this.right == null){
            context.error(this,"Missing operand.");
            return;
        }

        this.left.validate(context);
        this.right.validate(context);

        List<Type> operandTypes = operandTypes();
        if(operandTypes.size() != 2){
            context.error(this,"Wrong number of operands.");
        }

        Type leftType = operandTypes.get(0);
        Type rightType = operandTypes.get(1);
        if(!(this.left.getReturnType() == leftType || this.left.getReturnType() == TypeEnum.ANY))
            context.error(this,"Invalid operand type. Expected: " + leftType + " but got: " + this.left.getReturnType());

        if(!(this.right.getReturnType() == rightType || this.left.getReturnType() == TypeEnum.ANY))
            context.error(this,"Invalid operand type. Expected: " + rightType + " but got: " + this.right.getReturnType());
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
