package at.sfischer.constraints.model;

public class NullLiteral extends Value<Void> {

    public static final NullLiteral INSTANCE = new NullLiteral();

    private NullLiteral() {
        super(null);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.ANY;
    }
}
