package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;

public class Min extends ArrayAggregation {

    private static final String FUNCTION_NAME = "arrays.min";

    public Min(Node array) {
        this(array, new Variable(ELEMENT_NAME));
    }

    public Min(Node array, Node keySelector) {
        super(FUNCTION_NAME, array, keySelector);
    }

    @Override
    public Node evaluate() {
        Percentile percentile = new Percentile(
                getParameter(0),
                getParameter(1),
                new NumberLiteral(0));

        Node percentileResult = percentile.evaluate();

        if(percentileResult instanceof NumberLiteral numberLiteral){
            return percentileResult;
        }

        return this;
    }

    @Override
    protected ArrayAggregation createArrayAggregation(Node array, Node keySelector, Node... parameters) {
        return new Min(array, keySelector);
    }
}
