package at.sfischer.constraints.model.operators.logic;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.operators.UnaryOperator;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.List;

public abstract class LogicalUnaryOperator extends UnaryOperator {

    public LogicalUnaryOperator(Node operand) {
        super(operand);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }

    @Override
    public void validate(ValidationContext context) {
        if(this.operand == null){
            context.error(this,"Missing operand.");
            return;
        }

        this.operand.validate(context);

        if (!(this.operand.getReturnType() == TypeEnum.BOOLEAN || this.operand.getReturnType() == TypeEnum.ANY))
            context.error(this,"Invalid operand type.");
    }

    protected Boolean getBoolean() {
        Node operand = this.operand.evaluate();
        if (operand instanceof BooleanLiteral) {
            return ((BooleanLiteral) operand).getValue();
        }

        return null;
    }

    @Override
    public List<Node> getChildren() {
        return List.of(operand);
    }

    @Override
    public List<Type> operandTypes() {
        return List.of(TypeEnum.BOOLEAN);
    }
}
