package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class Distinct extends ArrayOperation {

    private static final String FUNCTION_NAME = "arrays.distinct";

    private Type arrayElementType = TypeEnum.ANY;

    public Distinct(Node array) {
        super(FUNCTION_NAME, array);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);

        if (arrayValues == null) {
            return this;
        }

        arrayElementType = arrayValues.getElementType();

        LinkedHashSet<Value<?>> distinctValues = new LinkedHashSet<>();
        Collections.addAll(distinctValues, arrayValues.getValue());

        return new ArrayValues<>(
                arrayValues.getElementType(),
                distinctValues.toArray(new Value<?>[0])
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
        return new Distinct(
                getParameter(0).setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new Distinct(
                getParameter(0).cloneNode()
        );
    }
}
