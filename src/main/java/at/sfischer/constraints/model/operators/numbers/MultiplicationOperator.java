package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;

import java.util.Map;

public class MultiplicationOperator extends ArithmeticOperator {

    public MultiplicationOperator(Node left, Node right) {
        super(left, right);
    }

    @Override
    public Node evaluate() {
        Number left = this.getLeftNumber();
        Number right = this.getRightNumber();
        if(left != null && right != null){
            double result = left.doubleValue() * right.doubleValue();
            return new NumberLiteral(result);
        }

        if(left != null){
            if(left.doubleValue() == 0){
                return new NumberLiteral(0);
            }

            return new MultiplicationOperator(new NumberLiteral(left), this.right);
        }

        if(right != null){
            if(right.doubleValue() == 0){
                return new NumberLiteral(0);
            }

            return new MultiplicationOperator(this.left, new NumberLiteral(right));
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new MultiplicationOperator(this.left.setVariableValues(values), this.right.setVariableValues(values));
    }
}
