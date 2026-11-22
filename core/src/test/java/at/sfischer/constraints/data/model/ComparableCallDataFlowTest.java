package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComparableCallDataFlowTest {

    @Test
    void aCounterexampleRejectsTheMappingForEveryComparableOccurrence() {
        Graph graph = new Graph();
        GraphNode first = graph.getOrCreateNode(new GraphNode("first", "first"));
        GraphNode second = graph.getOrCreateNode(new GraphNode("second", "second"));
        GraphNode third = graph.getOrCreateNode(new GraphNode("third", "third"));
        AsynchronousCallEdge source = new AsynchronousCallEdge(first, second);
        AsynchronousCallEdge target = new AsynchronousCallEdge(second, third);
        source.addData("trace", DataObject.parseData("{id:'A',stable:'kept'}"), null);
        source.addData("trace", DataObject.parseData("{id:'B',stable:'kept'}"), null);
        target.addData("trace", DataObject.parseData("{id:'A',stable:'kept'}"), null);
        target.addData("trace", DataObject.parseData("{id:'C',stable:'kept'}"), null);
        graph.addCallRelation(new CallRelation("trace", source, 0, target, 0));
        graph.addCallRelation(new CallRelation("trace", source, 1, target, 1));

        graph.finalizeGraph();

        DataSchemaEntry<SimpleDataSchema> id = source.getSchema().findDataSchemaEntry("id");
        DataSchemaEntry<SimpleDataSchema> stable = source.getSchema().findDataSchemaEntry("stable");
        DataSchemaEntry<SimpleDataSchema> targetStable = target.getSchema().findDataSchemaEntry("stable");
        assertEquals(2, source.getDataFlowsTo(target).size());
        for (DataFlow flow : source.getDataFlowsTo(target)) {
            assertFalse(flow.getDataFlows().containsKey(id));
            assertSame(targetStable, flow.getDataFlows().get(stable));
            assertEquals(flow.getSourceRow(), flow.getTargetRow());
        }
        assertTrue(source.getFrom().deriveDataPaths(id).getRoots().isEmpty());
    }
}
