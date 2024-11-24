package at.sfischer.constraints.model;

public class BooleanLiteral extends Value<Boolean> {

    public static final BooleanLiteral TRUE = new BooleanLiteral(true);
    public static final BooleanLiteral FALSE = new BooleanLiteral(false);

    public static BooleanLiteral getBooleanLiteral(boolean value){
        return value ? TRUE : FALSE;
    }

    private BooleanLiteral(boolean value) {
        super(value);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }
}
