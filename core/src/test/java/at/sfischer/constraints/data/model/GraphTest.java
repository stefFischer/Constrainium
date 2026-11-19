package at.sfischer.constraints.data.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphTest {

    @Test
    void legacyNodesKeepUnknownTypeAndStillMergeById() {
        Graph graph = new Graph();
        GraphNode original = graph.getOrCreateNode(new GraphNode("node", "original"));
        GraphNode duplicate = new GraphNode("node", "another name");

        graph.addEntryNode(duplicate);

        assertEquals("unknown", original.getType());
        assertSame(original, graph.getOrCreateNode(duplicate));
        assertSame(original, graph.getEntryNodes().iterator().next());
        assertEquals(1, graph.getNodes().size());
    }

    @Test
    void nodesWithTheSameIdAndTypeKeepTheOriginalInstanceAndEdges() {
        Graph graph = new Graph();
        GraphNode original = graph.getOrCreateNode(new GraphNode("service", "Service", "custom-service"));
        GraphNode endpoint = graph.getOrCreateNode(new GraphNode("endpoint", "Endpoint", "endpoint"));
        Edge edge = new DependencyEdge(original, endpoint);

        GraphNode merged = graph.getOrCreateNode(new GraphNode("service", "Renamed", "custom-service"));

        assertSame(original, merged);
        assertEquals("custom-service", merged.getType());
        assertSame(edge, merged.getOutgoing().getFirst());
        assertSame(original, edge.getFrom());
    }

    @ParameterizedTest
    @ValueSource(strings = {"database", "unknown"})
    void rejectsConflictingTypesWithoutReplacingTheRegisteredNode(String conflictingType) {
        Graph graph = new Graph();
        GraphNode original = graph.getOrCreateNode(new GraphNode("shared", "Service", "service"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                graph.addEntryNode(new GraphNode("shared", "Other", conflictingType)));

        assertTrue(exception.getMessage().contains("shared"));
        assertTrue(exception.getMessage().contains(conflictingType));
        assertSame(original, graph.getNode("shared"));
        assertTrue(graph.getEntryNodes().isEmpty());
        assertEquals(1, graph.getNodes().size());
    }
}
