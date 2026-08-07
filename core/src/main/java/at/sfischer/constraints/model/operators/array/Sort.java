package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.*;

public class Sort extends ArrayOperation {

    private static final String FUNCTION_NAME = "arrays.sort";

    private Type arrayElementType = TypeEnum.ANY;

    public Sort(Node array) {
        this(
                array,
                new Variable(ELEMENT_NAME),
                BooleanLiteral.TRUE
        );
    }

    public Sort(Node array, Node ascending) {
        this(
                array,
                new Variable(ELEMENT_NAME),
                ascending
        );
    }

    public Sort(Node array, Node keySelector, Node ascending) {
        super(FUNCTION_NAME, array, keySelector, ascending);
    }

    @Override
    public Node evaluate() {
        ArrayValues<?> arrayValues = getArrayArgument(0);
        Node keySelector = getParameter(1);
        Node ascendingNode = getParameter(2).evaluate();

        if (arrayValues == null) {
            return this;
        }

        arrayElementType = arrayValues.getElementType();

        if (!(ascendingNode instanceof BooleanLiteral ascLiteral)) {
            return this;
        }

        boolean ascending = ascLiteral.getValue();
        List<Value<?>> values = new ArrayList<>(Arrays.asList(arrayValues.getValue()));
        Comparator<Value<?>> comparator = (left, right) -> {
            Node leftKey = keySelector
                    .setVariableNameValue(ELEMENT_NAME, left)
                    .evaluate();
            Node rightKey = keySelector
                    .setVariableNameValue(ELEMENT_NAME, right)
                    .evaluate();

            if (!(leftKey instanceof Value<?> leftValue) ||
                    !(rightKey instanceof Value<?> rightValue)) {
                throw new IllegalStateException();
            }

            Comparable<Object> l = asComparable(leftValue);
            Object r = rightValue.getValue();

            int result = l.compareTo(r);
            return ascending ? result : -result;
        };

        try {
            values.sort(comparator);
        } catch (Exception ex) {
            return this;
        }

        return new ArrayValues<>(
                arrayValues.getElementType(),
                values.toArray(new Value<?>[0])
        );
    }

    @SuppressWarnings("unchecked")
    private Comparable<Object> asComparable(Value<?> value) {
        Object obj = value.getValue();
        if (!(obj instanceof Comparable<?> comparable)) {
            throw new IllegalArgumentException();
        }

        return (Comparable<Object>) comparable;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        Node array = getParameter(0).evaluate();
        Node keySelector = getParameter(1);
        Node ascending = getParameter(2);

        if (array.getReturnType() == TypeEnum.ANY) {
            return;
        }

        if (ascending.getReturnType() != TypeEnum.BOOLEAN) {
            context.error(this, "Sort ascending flag must be a boolean.");
        }

        Type keyType = keySelector.getReturnType();
        if (!(keyType == TypeEnum.ANY ||
                keyType == TypeEnum.INTEGER ||
                keyType == TypeEnum.NUMBER ||
                keyType == TypeEnum.STRING)) {

            context.error(this, "Sort key selector must return INTEGER, NUMBER or STRING.");
        }

        if (!(array instanceof ArrayValues<?> arrayValues)) {
            return;
        }

        arrayElementType = arrayValues.getElementType();

        Type elementVariableType;
        if(keySelector.equals(new Variable(ELEMENT_NAME))) {
            elementVariableType = TypeEnum.ANY;
        } else {
            Map<Variable, Type> variableTypes = keySelector.inferVariableTypes();
            elementVariableType = variableTypes.get(new Variable(ELEMENT_NAME));
            if (elementVariableType == null) {
                context.error(this, "Sort key selector must reference the array element variable.");
                return;
            }
        }

        if (!arrayElementType.canAssignTo(elementVariableType)) {
            context.error(this, "Sort key selector expects incompatible array element type.");
        }
    }

    @Override
    public List<Node> getChildren() {
        return List.of(
                getParameter(0),
                getParameter(1),
                getParameter(2)
        );
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        Node array = getParameter(0);
        Node selector = getParameter(1);
        Map<Variable, Node> newValues = new HashMap<>(values);
        newValues.remove(new Variable(ELEMENT_NAME));

        return new Sort(
                array.setVariableValues(values),
                selector.setVariableValues(newValues),
                getParameter(2).setVariableValues(values)
        );
    }

    @Override
    public Node cloneNode() {
        return new Sort(
                getParameter(0).cloneNode(),
                getParameter(1).cloneNode(),
                getParameter(2).cloneNode()
        );
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(
                new ArrayType(arrayElementType),
                TypeEnum.ANY,
                TypeEnum.BOOLEAN
        );
    }

    @Override
    public Type getReturnType() {
        return new ArrayType(arrayElementType);
    }
}
