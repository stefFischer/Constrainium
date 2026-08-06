package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Concat extends ArrayOperation {

    private static final String FUNCTION_NAME = "arrays.concat";

    private Type arrayElementType = TypeEnum.ANY;

    public Concat(Node array1, Node array2) {
        super(FUNCTION_NAME, array1, array2);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> array1 = getArrayArgument(0);
        ArrayValues<?> array2 = getArrayArgument(1);

        if (array1 == null || array2 == null) {
            return this;
        }

        if (!array2.getElementType().canAssignTo(array1.getElementType())) {
            return this;
        }

        arrayElementType = array1.getElementType();

        List<Value<?>> values = new ArrayList<>();

        values.addAll(Arrays.asList(array1.getValue()));
        values.addAll(Arrays.asList(array2.getValue()));

        return new ArrayValues<>(
                array1.getElementType(),
                values.toArray(new Value<?>[0])
        );
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        Node array1 = getParameter(0).evaluate();
        Node array2 = getParameter(1).evaluate();

        if (array1.getReturnType() == TypeEnum.ANY ||
                array2.getReturnType() == TypeEnum.ANY) {
            return;
        }

        if (!(array1 instanceof ArrayValues<?> firstArray)) {
            context.error(this, "Concat requires the first parameter to be an array.");
            return;
        }

        if (!(array2 instanceof ArrayValues<?> secondArray)) {
            context.error(this, "Concat requires the second parameter to be an array.");
            return;
        }

        if (!secondArray.getElementType().canAssignTo(firstArray.getElementType())) {
            context.error(this, "Concat arrays need compatible element types.");
            return;
        }

        arrayElementType = firstArray.getElementType();
    }

    @Override
    public List<Node> getChildren() {
        return List.of(
                getParameter(0),
                getParameter(1)
        );
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(
                new ArrayType(arrayElementType),
                new ArrayType(arrayElementType)
        );
    }

    @Override
    public Type getReturnType() {
        return new ArrayType(arrayElementType);
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new Concat(
                getParameter(0).setVariableValues(values),
                getParameter(1).setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new Concat(
                getParameter(0).cloneNode(),
                getParameter(1).cloneNode()
        );
    }
}
