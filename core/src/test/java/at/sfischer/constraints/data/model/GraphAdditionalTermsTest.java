package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.strings.SubString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphAdditionalTermsTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void siblingCallsUseAdditionalTermsToMatchResponsesToFollowingRequests(boolean asynchronousTarget) {
        Graph graph = new Graph();
        GraphNode caller = node(graph, "caller");
        SynchronousCallEdge source = new SynchronousCallEdge(caller, node(graph, "source"));
        CallEdge target = asynchronousTarget
                ? new AsynchronousCallEdge(caller, node(graph, "target"))
                : new SynchronousCallEdge(caller, node(graph, "target"));
        source.addData("trace", DataObject.parseData("{request:'prefix:WRONG'}"),
                DataObject.parseData("{returned:'prefix:A1'}"));
        target.addData("trace", DataObject.parseData("{selected:'A1',other:'WRONG'}"), null);
        graph.addCallRelation(new CallRelation("trace", source, 0, target, 0));

        graph.finalizeGraph(null, additionalTerms());

        String inputPrefix = asynchronousTarget ? "" : "input.";
        assertMapping(source, "output.returned", target, inputPrefix + "selected");
        assertNoMapping(source, "input.request", target);
        assertTrue(target.getDataFlowsTo(source).isEmpty());
        assertTrue(graph.isFinalized());
        assertTrue(source.isFrozen());
        assertTrue(target.getDataByTrace().isEmpty());
        assertTrue(source.getDataByTrace().isEmpty());
        assertThrows(IllegalStateException.class, () -> source.addData("late", new DataObject(), null));
    }

    @Test
    void nestedCallsApplyAdditionalTermsToRequestsAndReturningResponses() {
        Graph graph = new Graph();
        GraphNode service = node(graph, "service");
        SynchronousCallEdge parent = new SynchronousCallEdge(node(graph, "caller"), service);
        DatabaseQueryEdge child = new DatabaseQueryEdge(service, node(graph, "database"), "lookup");
        parent.addData("trace", DataObject.parseData("{encoded:'prefix:A1'}"),
                DataObject.parseData("{receipt:'prefix:R1'}"));
        child.addData("trace", DataObject.parseData("{id:'A1'}"), DataObject.parseData("{receiptId:'R1'}"));
        graph.addCallRelation(new CallRelation("trace", parent, 0, child, 0));

        graph.finalizeGraph(null, additionalTerms());

        assertMapping(parent, "input.encoded", child, "input.id");
        assertMapping(child, "output.receiptId", parent, "output.receipt");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void comparableExecutionsApplyNoViolationsPolicyToAdditionalTerms(boolean counterexample) {
        Graph graph = new Graph();
        GraphNode service = node(graph, "service");
        AsynchronousCallEdge source = new AsynchronousCallEdge(node(graph, "caller"), service);
        AsynchronousCallEdge target = new AsynchronousCallEdge(service, node(graph, "target"));
        source.addData("first", DataObject.parseData("{encoded:'prefix:A1',stable:'unchanged'}"), null);
        target.addData("first", DataObject.parseData("{id:'A1',copy:'unchanged'}"), null);
        source.addData("second", DataObject.parseData("{encoded:'prefix:B2',stable:'unchanged'}"), null);
        target.addData("second", DataObject.parseData(counterexample
                ? "{id:'OTHER',copy:'unchanged'}" : "{id:'B2',copy:'unchanged'}"), null);
        graph.addCallRelationGroup(List.of(new CallRelation("first", source, 0, target, 0),
                new CallRelation("second", source, 0, target, 0)));

        graph.finalizeGraph(null, additionalTerms());

        assertEquals(2, source.getDataFlowsTo(target).size());
        assertEquals(List.of("first", "second"), source.getDataFlowsTo(target).stream()
                .map(DataFlow::getTraceId).toList());
        if (counterexample) {
            assertNoMapping(source, "encoded", target);
        } else {
            assertMapping(source, "encoded", target, "id");
        }
        assertMapping(source, "stable", target, "copy");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void mergedEdgesKeepOccurrencePairingWhenUsingAdditionalTerms(boolean swappedTargets) {
        Graph graph = new Graph();
        GraphNode service = node(graph, "service");
        AsynchronousCallEdge source = new AsynchronousCallEdge(node(graph, "caller"), service);
        AsynchronousCallEdge target = new AsynchronousCallEdge(service, node(graph, "target"));
        source.addData("trace", DataObject.parseData("{encoded:'prefix:A1'}"), null);
        source.addData("trace", DataObject.parseData("{encoded:'prefix:B2'}"), null);
        target.addData("trace", DataObject.parseData(swappedTargets ? "{id:'B2'}" : "{id:'A1'}"), null);
        target.addData("trace", DataObject.parseData(swappedTargets ? "{id:'A1'}" : "{id:'B2'}"), null);
        graph.addCallRelation(new CallRelation("trace", source, 0, target, 0));
        graph.addCallRelation(new CallRelation("trace", source, 1, target, 1));

        graph.finalizeGraph(null, additionalTerms());

        assertEquals(2, source.getDataFlowsTo(target).size());
        for (int index = 0; index < 2; index++) {
            DataFlow flow = source.getDataFlowsTo(target).get(index);
            assertEquals(index, flow.getSourceRow());
            assertEquals(index, flow.getTargetRow());
        }
        if (swappedTargets) {
            assertNoMapping(source, "encoded", target);
        } else {
            assertMapping(source, "encoded", target, "id");
        }
    }

    private static List<Node> additionalTerms() {
        return List.of(new SubString(new Variable("a"), new Variable("b")));
    }

    private static GraphNode node(Graph graph, String id) {
        return graph.getOrCreateNode(new GraphNode(id, id));
    }

    private static void assertMapping(CallEdge source, String sourcePath, CallEdge target, String targetPath) {
        DataSchemaEntry<SimpleDataSchema> sourceField = source.getSchema().findDataSchemaEntry(sourcePath);
        DataSchemaEntry<SimpleDataSchema> targetField = target.getSchema().findDataSchemaEntry(targetPath);
        assertNotNull(sourceField);
        assertNotNull(targetField);
        assertFalse(source.getDataFlowsTo(target).isEmpty());
        for (DataFlow flow : source.getDataFlowsTo(target)) {
            assertSame(targetField, flow.getDataFlows().get(sourceField));
        }
    }

    private static void assertNoMapping(CallEdge source, String sourcePath, CallEdge target) {
        DataSchemaEntry<SimpleDataSchema> sourceField = source.getSchema().findDataSchemaEntry(sourcePath);
        assertNotNull(sourceField);
        for (DataFlow flow : source.getDataFlowsTo(target)) {
            assertFalse(flow.getDataFlows().containsKey(sourceField));
        }
    }
}
