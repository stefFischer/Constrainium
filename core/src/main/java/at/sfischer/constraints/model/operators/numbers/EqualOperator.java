package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;

import java.util.Map;

public class EqualOperator extends NumberComparisonOperator {

    public EqualOperator(Node left, Node right) {
        super(left, right);
    }

    @Override
    public Node evaluate() {
        Number left = this.getLeftNumber();
        Number right = this.getRightNumber();

        // Numbers could be resolved.
        if(left != null && right != null){
            boolean result = left.doubleValue() == right.doubleValue();
            return BooleanLiteral.getBooleanLiteral(result);
        }

        if(this.left.equals(this.right)){
            return BooleanLiteral.TRUE;
        }

        if(left != null){
            return new EqualOperator(new NumberLiteral(left), this.right);
        }

        if(right != null){
            return new EqualOperator(this.left, new NumberLiteral(right));
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new EqualOperator(this.left.setVariableValues(values), this.right.setVariableValues(values));
    }}
