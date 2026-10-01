package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenerateArray extends ArrayOperation {

    private static final String FUNCTION_NAME = "arrays.generate";

    public GenerateArray(Node generator, Node length) {
        super(FUNCTION_NAME, generator, length);
    }

    @Override
    public Node evaluate() {
        Node generator = getParameter(0);
        Node lengthNode = getParameter(1).evaluate();
        if (!(lengthNode instanceof Value<?> value) || !(value.getValue() instanceof Integer length)) {
            return this;
        }

        if (length < 0) {
            return this;
        }

        Type elementType = generator.getReturnType();
        List<Value<?>> values = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            Node generated = generator.setVariableNameValue(INDEX_NAME, new IntegerLiteral(index)).evaluate();
            if (!(generated instanceof Value<?> generatedValue)) {
                return this;
            }

            values.add(generatedValue);
        }

        return new ArrayValues<>(elementType, values.toArray(new Value<?>[0]));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.ANY, TypeEnum.INTEGER);
    }

    @Override
    public Type getReturnType() {
        Type elementType = getParameter(0).getReturnType();
        return new ArrayType(elementType);
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new GenerateArray(getParameter(0).setVariableValues(values), getParameter(1).setVariableValues(values));
    }

    @Override
    public Node cloneNode() {
        return new GenerateArray(getParameter(0).cloneNode(), getParameter(1).cloneNode());
    }
}
