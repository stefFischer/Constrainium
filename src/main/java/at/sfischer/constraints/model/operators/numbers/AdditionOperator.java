package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.Literal;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;

import java.util.Map;

public class AdditionOperator extends ArithmeticOperator {

    public AdditionOperator(Node left, Node right) {
        super(left, right);
    }

    @Override
    public Node evaluate() {
        Number left = this.getLeftNumber();
        Number right = this.getRightNumber();
        if(left != null && right != null){
            double result = left.doubleValue() + right.doubleValue();
            return new NumberLiteral(result);
        }

        if(left != null){
            if(left.doubleValue() == 0){
                return this.right;
            }

            return new AdditionOperator(new NumberLiteral(left), this.right);
        }

        if(right != null){
            if(right.doubleValue() == 0){
                return this.left;
            }

            return new AdditionOperator(this.left, new NumberLiteral(right));
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Literal<?>> values) {
        return new AdditionOperator(this.left.setVariableValues(values), this.right.setVariableValues(values));
    }
}
