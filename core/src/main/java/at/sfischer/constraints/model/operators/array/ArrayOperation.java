package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;

public abstract class ArrayOperation extends Function {
    public static final String ELEMENT_NAME = "ARRAY_ELEMENT";
    public static final String INDEX_NAME = "ARRAY_INDEX";

    public static final String LEFT_ELEMENT_NAME = "ARRAY_LEFT";
    public static final String RIGHT_ELEMENT_NAME = "ARRAY_RIGHT";

    public ArrayOperation(String name, List<Node> parameters) {
        super(name, parameters);
    }

    public ArrayOperation(String name, Node... parameters) {
        super(name, parameters);
    }
}
