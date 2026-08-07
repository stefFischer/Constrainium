package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;

public class Product extends ArrayAggregation {

    private static final String FUNCTION_NAME = "arrays.product";

    public Product(Node array) {
        this(array, new Variable(ELEMENT_NAME));
    }

    public Product(Node array, Node keySelector) {
        super(FUNCTION_NAME, array, keySelector);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node keySelector = getParameter(1);

        if (arrayValues == null) {
            return this;
        }

        double product = 1;
        boolean integer = true;

        for (Value<?> element : arrayValues.getValue()) {
            Node key = keySelector
                    .setVariableNameValue(ELEMENT_NAME, element)
                    .evaluate();

            if (!(key instanceof Value<?> value)) {
                return this;
            }

            if (value.getReturnType() == TypeEnum.INTEGER) {
                product *= ((Number) value.getValue()).longValue();
            } else if (value.getReturnType() == TypeEnum.NUMBER) {
                product *= ((Number) value.getValue()).doubleValue();
                integer = false;
            } else {
                return this;
            }
        }

        if (integer) {
            return new IntegerLiteral((int) product);
        }

        return new NumberLiteral(product);
    }

    @Override
    protected ArrayAggregation createArrayAggregation(Node array, Node keySelector, Node... parameters) {
        return new Product(
                array,
                keySelector
        );
    }

    @Override
    public Type getReturnType() {
        Node selector = getParameter(1);
        Type type = selector != null ? selector.getReturnType() : TypeEnum.NUMBER;

        if (type == TypeEnum.INTEGER) {
            return TypeEnum.INTEGER;
        }

        return TypeEnum.NUMBER;
    }
}
