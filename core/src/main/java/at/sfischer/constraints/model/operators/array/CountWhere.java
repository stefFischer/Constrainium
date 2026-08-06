package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;

public class CountWhere extends ArrayQuantifier {

    private static final String FUNCTION_NAME = "arrays.countWhere";

    public CountWhere(Node array, Node predicate) {
        super(FUNCTION_NAME, array, predicate);
    }

    @Override
    protected ArrayQuantifier createArrayQuantifier(Node array, Node condition) {
        return new CountWhere(array, condition);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node predicate = getParameter(1);
        if (arrayValues != null) {
            int count = 0;
            for (Value<?> element : arrayValues.getValue()) {
                Node cond = predicate.setVariableNameValue(ELEMENT_NAME, element);
                Node result = cond.evaluate();

                if (result instanceof BooleanLiteral bool) {
                    if (bool.getValue()) {
                        count++;
                    }
                } else {
                    return this;
                }
            }

            return new IntegerLiteral(count);
        }

        return this;
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.INTEGER;
    }
}
