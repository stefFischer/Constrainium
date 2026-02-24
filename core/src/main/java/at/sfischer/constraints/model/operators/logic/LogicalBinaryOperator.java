package at.sfischer.constraints.model.operators.logic;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.operators.BinaryOperator;

import java.util.List;

public abstract class LogicalBinaryOperator extends BinaryOperator {

    public LogicalBinaryOperator(Node left, Node right) {
        super(left, right);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }

    protected Boolean getLeftBoolean(){
        Node left = this.left.evaluate();
        this.left = left;
        if(left instanceof BooleanLiteral){
            return ((BooleanLiteral) left).getValue();
        }

        return null;
    }

    protected Boolean getRightBoolean() {
        Node right = this.right.evaluate();
        this.right = right;
        if (right instanceof BooleanLiteral) {
            return ((BooleanLiteral) right).getValue();
        }

        return null;
    }

    @Override
    public List<Node> getChildren() {
        return List.of(left, right);
    }

    @Override
    public List<Type> operandTypes() {
        return List.of(TypeEnum.BOOLEAN, TypeEnum.BOOLEAN);
    }
}
