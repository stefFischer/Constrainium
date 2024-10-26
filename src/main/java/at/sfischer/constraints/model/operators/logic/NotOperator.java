package at.sfischer.constraints.model.operators.logic;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Literal;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;

import java.util.Map;

public class NotOperator extends LogicalUnaryOperator {

    public NotOperator(Node operand) {
        super(operand);
    }

    @Override
    public Node evaluate() {
        Boolean operand = this.getBoolean();
        if(operand != null){
            boolean result = !operand;
            return new BooleanLiteral(result);
        }

        if(this.operand instanceof NotOperator){
            return ((NotOperator) this.operand).operand;
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Literal<?>> values) {
        return new NotOperator(this.operand.setVariableValues(values));
    }
}
