package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.miner.MinApplicationsPolicy;
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

class GraphRetentionPolicyTest {

    @Test
    void customPolicyEvaluatesEachComparableGroupSeparatelyOnMergedEdges() {
        Graph graph = new Graph();
        GraphNode service = node(graph, "service");
        AsynchronousCallEdge source = new AsynchronousCallEdge(node(graph, "caller"), service);
        AsynchronousCallEdge target = new AsynchronousCallEdge(service, node(graph, "target"));
        List<CallRelation> accepted = List.of(
                observation("first", source, target, "{id:'A1',encoded:'prefix:X1'}", "{copy:'A1',selected:'X1'}"),
                observation("first", source, target, "{id:'B2',encoded:'prefix:Y2'}", "{copy:'B2',selected:'Y2'}"),
                observation("second", source, target, "{id:'C3',encoded:'prefix:Z3'}", "{copy:'OTHER',selected:'OTHER'}")
        );
        List<CallRelation> rejected = List.of(
                observation("first", source, target, "{id:'D4',encoded:'prefix:U4'}", "{copy:'D4',selected:'U4'}"),
                observation("first", source, target, "{id:'E5',encoded:'prefix:V5'}", "{copy:'OTHER',selected:'OTHER'}"),
                observation("second", source, target, "{id:'F6',encoded:'prefix:W6'}", "{copy:'OTHER',selected:'OTHER'}")
        );
        graph.addCallRelationGroup(accepted);
        graph.addCallRelationGroup(rejected);

        graph.finalizeGraph(null, additionalTerms(), new MinApplicationsPolicy(2));

        List<DataFlow> flows = source.getDataFlowsTo(target);
        assertEquals(6, flows.size());
        for (int index = 0; index < accepted.size(); index++) {
            DataFlow flow = flows.get(index);
            CallRelation relation = accepted.get(index);
            assertOccurrence(flow, relation);
            assertMapping(flow, source, "id", target, "copy");
            assertMapping(flow, source, "encoded", target, "selected");
        }
        for (int index = 0; index < rejected.size(); index++) {
            DataFlow flow = flows.get(accepted.size() + index);
            assertOccurrence(flow, rejected.get(index));
            assertTrue(flow.getDataFlows().isEmpty());
        }
        assertTrue(graph.isFinalized());
        assertTrue(source.isFrozen());
        assertTrue(target.isFrozen());
        assertTrue(source.getDataByTrace().isEmpty());
        assertTrue(target.getDataByTrace().isEmpty());
        assertThrows(IllegalStateException.class, () -> source.addData("late", new DataObject(), null));
    }

    @Test
    void nestedCallsUseTheCustomPolicyForForwardRequestsAndReturningResponses() {
        Graph graph = new Graph();
        GraphNode service = node(graph, "service");
        SynchronousCallEdge parent = new SynchronousCallEdge(node(graph, "caller"), service);
        DatabaseQueryEdge child = new DatabaseQueryEdge(service, node(graph, "database"), "lookup");
        for (int index = 0; index < 3; index++) {
            String traceId = "trace-" + index;
            parent.addData(traceId, DataObject.parseData("{encoded:'prefix:A" + index + "'}"),
                    DataObject.parseData("{receipt:'prefix:R" + index + "'}"));
            child.addData(traceId, DataObject.parseData(index < 2 ? "{id:'A" + index + "'}" : "{id:'OTHER'}"),
                    DataObject.parseData(index < 2 ? "{receiptId:'R" + index + "'}" : "{receiptId:'OTHER'}"));
        }
        graph.addCallRelationGroup(List.of(new CallRelation("trace-0", parent, 0, child, 0),
                new CallRelation("trace-1", parent, 0, child, 0), new CallRelation("trace-2", parent, 0, child, 0)));

        graph.finalizeGraph(null, additionalTerms(), new MinApplicationsPolicy(2));

        assertEquals(3, parent.getDataFlowsTo(child).size());
        assertEquals(3, child.getDataFlowsTo(parent).size());
        for (DataFlow flow : parent.getDataFlowsTo(child)) {
            assertMapping(flow, parent, "input.encoded", child, "input.id");
        }
        for (DataFlow flow : child.getDataFlowsTo(parent)) {
            assertMapping(flow, child, "output.receiptId", parent, "output.receipt");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void siblingCallsUseTheCustomPolicyForResponsesToFollowingRequests(boolean asynchronousTarget) {
        Graph graph = new Graph();
        GraphNode caller = node(graph, "caller");
        SynchronousCallEdge source = new SynchronousCallEdge(caller, node(graph, "source"));
        CallEdge target = asynchronousTarget
                ? new AsynchronousCallEdge(caller, node(graph, "target"))
                : new SynchronousCallEdge(caller, node(graph, "target"));
        for (int index = 0; index < 3; index++) {
            source.addData("trace", DataObject.parseData("{request:'prefix:WRONG'}"),
                    DataObject.parseData("{returned:'prefix:A" + index + "'}"));
            target.addData("trace", DataObject.parseData(index < 2
                    ? "{selected:'A" + index + "',other:'WRONG'}" : "{selected:'OTHER',other:'WRONG'}"), null);
            graph.addCallRelation(new CallRelation("trace", source, index, target, index));
        }

        graph.finalizeGraph(null, additionalTerms(), new MinApplicationsPolicy(2));

        List<DataFlow> flows = source.getDataFlowsTo(target);
        assertEquals(3, flows.size());
        String inputPrefix = asynchronousTarget ? "" : "input.";
        DataSchemaEntry<SimpleDataSchema> request = source.getSchema().findDataSchemaEntry("input.request");
        assertNotNull(request);
        for (int index = 0; index < flows.size(); index++) {
            DataFlow flow = flows.get(index);
            assertMapping(flow, source, "output.returned", target, inputPrefix + "selected");
            assertFalse(flow.getDataFlows().containsKey(request));
            assertEquals(index, flow.getSourceRow());
            assertEquals(index, flow.getTargetRow());
        }
        assertTrue(target.getDataFlowsTo(source).isEmpty());
    }

    private static CallRelation observation(String traceId, CallEdge source, CallEdge target,
                                             String sourceJson, String targetJson) {
        int sourceRow = source.addData(traceId, DataObject.parseData(sourceJson), null);
        int targetRow = target.addData(traceId, DataObject.parseData(targetJson), null);
        return new CallRelation(traceId, source, sourceRow, target, targetRow);
    }

    private static List<Node> additionalTerms() {
        return List.of(new SubString(new Variable("a"), new Variable("b")));
    }

    private static GraphNode node(Graph graph, String id) {
        return graph.getOrCreateNode(new GraphNode(id, id));
    }

    private static void assertOccurrence(DataFlow flow, CallRelation relation) {
        assertEquals(relation.traceId(), flow.getTraceId());
        assertEquals(relation.sourceRow(), flow.getSourceRow());
        assertEquals(relation.targetRow(), flow.getTargetRow());
    }

    private static void assertMapping(DataFlow flow, CallEdge source, String sourcePath,
                                      CallEdge target, String targetPath) {
        DataSchemaEntry<SimpleDataSchema> sourceField = source.getSchema().findDataSchemaEntry(sourcePath);
        DataSchemaEntry<SimpleDataSchema> targetField = target.getSchema().findDataSchemaEntry(targetPath);
        assertNotNull(sourceField);
        assertNotNull(targetField);
        assertSame(targetField, flow.getDataFlows().get(sourceField));
    }
}
