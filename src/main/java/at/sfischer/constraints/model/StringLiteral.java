package at.sfischer.constraints.model;

public class StringLiteral extends Literal<String> {

    public StringLiteral(String value) {
        super(value);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.STRING;
    }
}
