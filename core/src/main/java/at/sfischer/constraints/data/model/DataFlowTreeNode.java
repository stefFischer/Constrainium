package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;

import java.util.IdentityHashMap;
import java.util.Map;

public class DataFlowTreeNode {

    private final GraphNode targetNode;

    private final Map<DataSchemaEntry<SimpleDataSchema>, DataFlowTreeNode> children = new IdentityHashMap<>();

    public DataFlowTreeNode(GraphNode targetNode) {
        this.targetNode = targetNode;
    }

    public GraphNode getTargetNode() { return targetNode; }

    public Map<DataSchemaEntry<SimpleDataSchema>, DataFlowTreeNode> getChildren() {
        return children;
    }

    protected void addChild(DataSchemaEntry<SimpleDataSchema> viaField, DataFlowTreeNode child) {
        children.put(viaField, child);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        buildString(sb, "");
        return sb.toString();
    }

    private void buildString(StringBuilder sb, String indent){
        sb.append(targetNode.getName());
        sb.append('(');
        sb.append(targetNode.getId());
        sb.append(')');
        sb.append('\n');

        for (Map.Entry<DataSchemaEntry<SimpleDataSchema>, DataFlowTreeNode> entry : children.entrySet()) {
            sb.append(indent);
            sb.append('\t');
            sb.append(entry.getKey().getQualifiedName());
            sb.append(" -> ");
            entry.getValue().buildString(sb, indent + "\t");
        }
    }
}
