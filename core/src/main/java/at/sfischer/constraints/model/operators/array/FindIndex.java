package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;

public class FindIndex extends ArrayQuantifier {

    private static final String FUNCTION_NAME = "arrays.findIndex";

    public FindIndex(Node array, Node predicate) {
        super(FUNCTION_NAME, array, predicate);
    }

    @Override
    protected ArrayQuantifier createArrayQuantifier(Node array, Node condition) {
        return new FindIndex(array, condition);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node predicate = getParameter(1);
        if (arrayValues != null) {
            Value<?>[] elements = arrayValues.getValue();
            for (int i = 0; i < elements.length; i++) {
                Node cond = predicate.setVariableNameValue(ELEMENT_NAME, elements[i]);
                Node result = cond.evaluate();

                if (result instanceof BooleanLiteral bool) {
                    if (bool.getValue()) {
                        return new IntegerLiteral(i);
                    }
                } else {
                    return this;
                }
            }

            return new IntegerLiteral(-1);
        }

        return this;
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.INTEGER;
    }
}
