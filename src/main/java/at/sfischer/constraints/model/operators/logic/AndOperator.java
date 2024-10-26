package at.sfischer.constraints.model.operators.logic;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Literal;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;

import java.util.Map;

public class AndOperator extends LogicalBinaryOperator {

    public AndOperator(Node left, Node right) {
        super(left, right);
    }

    @Override
    public Node evaluate() {
        Boolean left = this.getLeftBoolean();
        Boolean right = this.getRightBoolean();
        if(left != null && right != null){
            boolean result = left && right;
            return new BooleanLiteral(result);
        }

        if(this.left.equals(this.right)){
            return this.left;
        }

        if(left != null){
            if(!left){
                return new BooleanLiteral(false);
            }

            return this.right;
        }

        if(right != null){
            if(!right){
                return new BooleanLiteral(false);
            }

            return this.left;
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new AndOperator(this.left.setVariableValues(values), this.right.setVariableValues(values));
    }
}
