package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;

public class StandardDeviation extends ArrayAggregation {

    private static final String FUNCTION_NAME = "arrays.standardDeviation";

    public StandardDeviation(Node array) {
        this(array, new Variable(ELEMENT_NAME));
    }

    public StandardDeviation(Node array, Node keySelector) {
        super(FUNCTION_NAME, array, keySelector);
    }

    @Override
    public Node evaluate() {
        Variance variance = new Variance(getParameter(0), getParameter(1));
        Node varianceResult = variance.evaluate();

        if(varianceResult instanceof NumberLiteral numberLiteral){
            return new NumberLiteral(Math.sqrt(numberLiteral.getValue().doubleValue()));
        }

        return this;
    }

    @Override
    protected ArrayAggregation createArrayAggregation(Node array, Node keySelector, Node... parameters) {
        return new StandardDeviation(array, keySelector);
    }
}
