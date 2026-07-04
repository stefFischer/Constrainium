package at.sfischer.traces.otel.graph;

import at.sfischer.constraints.data.model.CallEdge;
import at.sfischer.constraints.data.model.GraphNode;

import java.util.List;

public record GraphFragment(
        GraphNode entryNode,
        GraphNode exitNode,
        List<CallEdge> internalEdges
) {
    public GraphFragment(GraphNode entryNode, GraphNode exitNode) {
        this(entryNode, exitNode, List.of());
    }

    /** Single-node fragment — entry and exit are the same node. */
    public GraphFragment(GraphNode node) {
        this(node, node, List.of());
    }
}
