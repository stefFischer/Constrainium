package at.sfischer.constraints.model;

public class NumberLiteral extends Literal<Number> {

    public NumberLiteral(Number value) {
        super(value);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.NUMBER;
    }
}
