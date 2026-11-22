package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;

import java.util.ArrayList;
import java.util.List;

public class DataPathNode {

    private final CallEdge edge;
    private final DataSchemaEntry<SimpleDataSchema> entry;

    private final String traceId;

    private final Integer row;

    private final List<DataPathNode> next = new ArrayList<>();

    public DataPathNode(CallEdge edge, DataSchemaEntry<SimpleDataSchema> entry) {
        this(edge, entry, null, null);
    }

    public DataPathNode(CallEdge edge, DataSchemaEntry<SimpleDataSchema> entry, String traceId, Integer row) {
        this.edge = edge;
        this.entry = entry;
        this.traceId = traceId;
        this.row = row;
    }

    public CallEdge getEdge() {
        return edge;
    }

    public DataSchemaEntry<SimpleDataSchema> getEntry() {
        return entry;
    }

    public String getTraceId() {
        return traceId;
    }

    public Integer getRow() {
        return row;
    }

    public List<DataPathNode> getNext() {
        return next;
    }

    public void addNext(DataPathNode child) {
        next.add(child);
    }
}
