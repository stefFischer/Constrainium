package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;

public class Find extends ArrayQuantifier {

    private static final String FUNCTION_NAME = "arrays.find";

    public Find(Node array, Node predicate) {
        super(FUNCTION_NAME, array, predicate);
    }

    @Override
    protected ArrayQuantifier createArrayQuantifier(Node array, Node condition) {
        return new Find(array, condition);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node predicate = getParameter(1);
        if (arrayValues != null) {
            for (Value<?> element : arrayValues.getValue()) {
                Node cond = predicate.setVariableNameValue(ELEMENT_NAME, element);
                Node result = cond.evaluate();

                if (result instanceof BooleanLiteral bool) {
                    if (bool.getValue()) {
                        return element;
                    }
                } else {
                    return this;
                }
            }

            return NullLiteral.INSTANCE;
        }

        return this;
    }

    @Override
    public Type getReturnType() {
        return arrayElementType;
    }
}
