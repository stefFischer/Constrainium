package at.sfischer.constraints.model;

public class IntegerLiteral extends NumberLiteral {

    public IntegerLiteral(Integer value) {
        super(value);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.INTEGER;
    }

    @Override
    public Node cloneNode() {
        return new IntegerLiteral((Integer)value);
    }

    @Override
    public Integer getValue() {
        return (Integer)super.getValue();
    }
}
