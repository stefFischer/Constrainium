package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;

import java.util.ArrayList;
import java.util.List;

public class Filter extends ArrayQuantifier {

    private static final String FUNCTION_NAME = "arrays.filter";

    public Filter(Node array, Node predicate) {
        super(FUNCTION_NAME, array, predicate);
    }

    @Override
    protected ArrayQuantifier createArrayQuantifier(Node array, Node condition) {
        return new Filter(array, condition);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node predicate = getParameter(1);

        if (arrayValues != null) {
            List<Value<?>> filteredValues = new ArrayList<>();
            for (Value<?> element : arrayValues.getValue()) {
                Node cond = predicate.setVariableNameValue(ELEMENT_NAME, element);
                Node result = cond.evaluate();

                if (result instanceof BooleanLiteral bool) {
                    if (bool.getValue()) {
                        filteredValues.add(element);
                    }
                } else {
                    return this;
                }
            }

            return new ArrayValues<>(
                    arrayValues.getElementType(),
                    filteredValues.toArray(new Value<?>[0])
            );
        }

        return this;
    }

    @Override
    public Type getReturnType() {
        return new ArrayType(arrayElementType);
    }
}
