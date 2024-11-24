package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.ArrayValues;
import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Value;
import at.sfischer.constraints.model.Node;

public class Exists extends ArrayQuantifier {

    private static final String FUNCTION_NAME = "arrays.exists";

    public Exists(Node array, Node condition) {
        super(FUNCTION_NAME, array, condition);
    }

    @Override
    protected ArrayQuantifier createArrayQuantifier(Node array, Node condition) {
        return new Exists(array, condition);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node condition = getParameter(1);

        if(arrayValues != null){
            Value<?>[] elements = arrayValues.getValue();
            for (Value<?> element : elements) {
                Node cond = condition.setVariableNameValue(ELEMENT_NAME, element);
                Node result = cond.evaluate();
                if(result instanceof BooleanLiteral){
                    if(((BooleanLiteral) result).getValue()){
                        return result;
                    }
                } else {
                    return this;
                }
            }

            return BooleanLiteral.FALSE;
        }

        return this;
    }
}
