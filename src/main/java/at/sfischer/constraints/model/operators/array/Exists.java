package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;

public class Exists extends ArrayQuantifier {

    private static final String FUNCTION_NAME = "arrays.exists";

    public Exists(Node array, Node condition) {
        super(FUNCTION_NAME, array, condition);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node condition = getParameter(1);

        if(arrayValues != null){
            Literal<?>[] elements = arrayValues.getValue();
            for (Literal<?> element : elements) {
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

            return new BooleanLiteral(false);
        }

        return this;
    }
}
