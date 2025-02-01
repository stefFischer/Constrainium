package at.sfischer.constraints.model;

public class StringLiteral extends Value<String> {

    public StringLiteral(String value) {
        super(value);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.STRING;
    }

    @Override
    public Node cloneNode() {
        return new StringLiteral(value);
    }
}
