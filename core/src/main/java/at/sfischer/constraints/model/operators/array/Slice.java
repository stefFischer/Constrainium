package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Slice extends ArrayOperation {

    private static final String FUNCTION_NAME = "arrays.slice";

    private Type arrayElementType = TypeEnum.ANY;

    public Slice(Node array, Node start, Node end) {
        super(FUNCTION_NAME, array, start, end);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);

        Node startNode = getParameter(1).evaluate();
        Node endNode = getParameter(2).evaluate();

        if (arrayValues == null) {
            return this;
        }

        arrayElementType = arrayValues.getElementType();

        if (!(startNode instanceof NumberLiteral startLiteral) ||
                !(endNode instanceof NumberLiteral endLiteral)) {
            return this;
        }

        int start = (int) startLiteral.getValue();
        int end = (int) endLiteral.getValue();

        Value<?>[] values = arrayValues.getValue();

        // Normalize indexes similar to common slice implementations.
        if (start < 0) {
            start = Math.max(values.length + start, 0);
        }
        if (end < 0) {
            end = Math.max(values.length + end, 0);
        }

        start = Math.min(start, values.length);
        end = Math.min(end, values.length);

        if (start > end) {
            return new ArrayValues<>(
                    arrayValues.getElementType(),
                    new Value<?>[0]
            );
        }

        Value<?>[] result = Arrays.copyOfRange(values, start, end);

        return new ArrayValues<>(
                arrayValues.getElementType(),
                result
        );
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        Node array = getParameter(0).evaluate();
        Node start = getParameter(1);
        Node end = getParameter(2);

        if (start == null || !start.getReturnType().canAssignTo(TypeEnum.NUMBER)) {
            context.error(this, "Slice start index must be a number.");
        }

        if (end == null || !end.getReturnType().canAssignTo(TypeEnum.NUMBER)) {
            context.error(this, "Slice end index must be a number.");
        }

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
        return List.of(
                getParameter(0),
                getParameter(1),
                getParameter(2)
        );
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(
                new ArrayType(arrayElementType),
                TypeEnum.NUMBER,
                TypeEnum.NUMBER
        );
    }

    @Override
    public Type getReturnType() {
        return new ArrayType(arrayElementType);
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new Slice(
                getParameter(0).setVariableValues(values),
                getParameter(1).setVariableValues(values),
                getParameter(2).setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new Slice(
                getParameter(0).cloneNode(),
                getParameter(1).cloneNode(),
                getParameter(2).cloneNode()
        );
    }
}
