package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Prepend extends ArrayOperation {

    private static final String FUNCTION_NAME = "arrays.append";

    private Type arrayElementType = TypeEnum.ANY;

    public Prepend(Node array, Node value) {
        super(FUNCTION_NAME, array, value);
        if(value != null){
            arrayElementType = value.getReturnType();
        }
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node value = getParameter(1).evaluate();

        if (arrayValues != null && value instanceof Value<?> val) {
            List<Value<?>> resultValues = new ArrayList<>(
                    Arrays.asList(arrayValues.getValue())
            );

            if (!val.getReturnType().canAssignTo(arrayValues.getElementType())) {
                return this;
            }

            resultValues.addFirst(val);

            return new ArrayValues<>(
                    arrayValues.getElementType(),
                    resultValues.toArray(new Value<?>[0])
            );
        }

        return this;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        Node array = getParameter(0).evaluate();
        Node value = getParameter(1);

        if (value == null) {
            context.error(this, "Prepend value cannot be null.");
            return;
        }

        if (array.getReturnType() == TypeEnum.ANY) {
            return;
        }
        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return;
        }

        if (!value.getReturnType().canAssignTo(arrayValues.getElementType())) {
            context.error(this, "Prepend value type must be compatible with the array element type.");
        }
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(
                new ArrayType(arrayElementType),
                arrayElementType
        );
    }

    @Override
    public Type getReturnType() {
        return new ArrayType(arrayElementType);
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        Node array = getParameter(0);
        Node value = getParameter(1);

        return new Prepend(
                array.setVariableValues(values),
                value.setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new Prepend(
                getParameter(0).cloneNode(),
                getParameter(1).cloneNode()
        );
    }
}