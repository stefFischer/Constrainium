package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.SimpleDataSchema;

public class CallEdge extends Edge {

    private final SimpleDataSchema schema;

    public CallEdge(GraphNode from, GraphNode to, SimpleDataSchema schema) {
        super(from, to);
        this.schema = schema;
    }

    public SimpleDataSchema getSchema() {
        return schema;
    }
}
