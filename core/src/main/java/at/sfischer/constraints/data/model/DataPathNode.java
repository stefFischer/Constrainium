package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;

import java.util.ArrayList;
import java.util.List;

public class DataPathNode {

    private final CallEdge edge;
    private final DataSchemaEntry<SimpleDataSchema> entry;

    private final List<DataPathNode> next = new ArrayList<>();

    public DataPathNode(CallEdge edge, DataSchemaEntry<SimpleDataSchema> entry) {
        this.edge = edge;
        this.entry = entry;
    }

    public CallEdge getEdge() {
        return edge;
    }

    public DataSchemaEntry<SimpleDataSchema> getEntry() {
        return entry;
    }

    public List<DataPathNode> getNext() {
        return next;
    }

    public void addNext(DataPathNode child) {
        next.add(child);
    }
}
