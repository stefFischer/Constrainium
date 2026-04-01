package at.sfischer.constraints.model.operators.logic;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;

import java.util.Map;

public class EquivalentOperator extends LogicalBinaryOperator {

    public EquivalentOperator(Node left, Node right) {
        super(left, right);
    }

    @Override
    public Node evaluate() {
        Boolean left = this.getLeftBoolean();
        Boolean right = this.getRightBoolean();
        if(left != null && right != null){
            boolean result = left == right;
            return BooleanLiteral.getBooleanLiteral(result);
        }

        if(this.left.equals(this.right)){
            return BooleanLiteral.TRUE;
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new EquivalentOperator(this.left.setVariableValues(values), this.right.setVariableValues(values));
    }
}
