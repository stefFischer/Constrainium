package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.*;

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
            return new BooleanLiteral(result);
        }

        if(this.left.equals(this.right)){
            return new BooleanLiteral(true);
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
