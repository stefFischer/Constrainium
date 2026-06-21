package at.sfischer.constraints.data.model;

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
}