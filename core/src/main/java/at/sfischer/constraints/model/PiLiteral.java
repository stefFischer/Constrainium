package at.sfischer.constraints.model;

public class PiLiteral extends NumberLiteral {
    public static final PiLiteral INSTANCE = new PiLiteral();

    private PiLiteral() {
        super(Math.PI);
    }
}
