package at.sfischer.constraints.model;

import at.sfischer.constraints.data.DataObject;

public class ComplexValue extends Literal<DataObject> {

    public ComplexValue(DataObject value) {
        super(value);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.COMPLEXTYPE;
    }
}
