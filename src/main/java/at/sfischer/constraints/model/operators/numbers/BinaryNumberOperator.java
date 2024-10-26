package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.operators.BinaryOperator;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Type;

import java.util.List;

public abstract class BinaryNumberOperator extends BinaryOperator {

    public BinaryNumberOperator(Node left, Node right) {
        super(left, right);
    }

    protected Number getLeftNumber(){
        Node left = this.left.evaluate();
        this.left = left;
        if(left instanceof NumberLiteral){
            return ((NumberLiteral) left).getValue();
        }

        return null;
    }

    protected Number getRightNumber() {
        Node right = this.right.evaluate();
        this.right = right;
        if (right instanceof NumberLiteral) {
            return ((NumberLiteral) right).getValue();
        }

        return null;
    }

    @Override
    public List<Node> getChildren() {
        return List.of(left, right);
    }

    @Override
    public List<Type> operandTypes() {
        return List.of(TypeEnum.NUMBER, TypeEnum.NUMBER);
    }
}
