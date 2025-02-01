package at.sfischer.constraints.model;

public class NumberLiteral extends Value<Number> {

    public NumberLiteral(Number value) {
        super(value);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.NUMBER;
    }

    @Override
    public Node cloneNode() {
        return new NumberLiteral(value);
    }
}
