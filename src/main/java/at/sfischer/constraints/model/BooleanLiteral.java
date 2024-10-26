package at.sfischer.constraints.model;

public class BooleanLiteral extends Literal<Boolean> {

    public BooleanLiteral(Boolean value) {
        super(value);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }
}
