package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.List;
import java.util.Map;

public class Reverse extends ArrayOperation {

    private static final String FUNCTION_NAME = "arrays.reverse";

    private Type arrayElementType = TypeEnum.ANY;

    public Reverse(Node array) {
        super(FUNCTION_NAME, array);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);

        if (arrayValues == null) {
            return this;
        }

        arrayElementType = arrayValues.getElementType();

        Value<?>[] values = arrayValues.getValue();
        Value<?>[] reversed = new Value<?>[values.length];

        for (int i = 0; i < values.length; i++) {
            reversed[values.length - 1 - i] = values[i];
        }

        return new ArrayValues<>(
                arrayValues.getElementType(),
                reversed
        );
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        Node array = getParameter(0).evaluate();
        if (array.getReturnType() == TypeEnum.ANY) {
            return;
        }

        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return;
        }

        arrayElementType = arrayValues.getElementType();
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(new ArrayType(arrayElementType));
    }

    @Override
    public Type getReturnType() {
        return new ArrayType(arrayElementType);
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new Reverse(
                getParameter(0).setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new Reverse(
                getParameter(0).cloneNode()
        );
    }
}
