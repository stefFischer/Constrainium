package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;

public abstract class ArrayOperation extends Function {
    public static final String ELEMENT_NAME = "ARRAY_ELEMENT";

    public ArrayOperation(String name, List<Node> parameters) {
        super(name, parameters);
    }

    public ArrayOperation(String name, Node... parameters) {
        super(name, parameters);
    }
}
