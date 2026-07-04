package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;

import java.util.*;

public class Graph {

    private final Set<GraphNode> entryNodes = new HashSet<>();

    private final Map<String, GraphNode> nodes = new HashMap<>();

    public GraphNode getOrCreateNode(GraphNode candidate) {
        return nodes.computeIfAbsent(
                candidate.getId(),
                id -> candidate
        );
    }

    public Collection<GraphNode> getNodes() {
        return nodes.values();
    }

    public GraphNode getNode(String id){
        return this.nodes.get(id);
    }

    public void addEntryNode(GraphNode candidate){
        GraphNode node = getOrCreateNode(candidate);
        this.entryNodes.add(node);
    }

    public Set<GraphNode> getEntryNodes() {
        return entryNodes;
    }

    public DataPaths deriveDataPaths(String qualifiedName) {
        List<DataPathNode> roots = new ArrayList<>();
        for (GraphNode entryNode : entryNodes) {
            for (Edge edge : entryNode.getOutgoing()) {
                if (!(edge instanceof CallEdge callEdge)) {
                    continue;
                }

                DataSchemaEntry<SimpleDataSchema> entry = callEdge.getSchema().findDataSchemaEntry(qualifiedName);
                if (entry == null) {
                    continue;
                }

                roots.addAll(entryNode.deriveDataPaths(entry).getRoots());
            }
        }

        return new DataPaths(roots);
    }
}