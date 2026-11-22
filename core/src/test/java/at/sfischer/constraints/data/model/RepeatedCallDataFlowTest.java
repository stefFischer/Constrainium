package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.data.model.utils.GraphJsonExporter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepeatedCallDataFlowTest {

    @Test
    void repeatedQueryPreservesDifferentRequestAndResponseMappingsWithinOneTrace() {
        Graph graph = new Graph();
        GraphNode client = graph.getOrCreateNode(new GraphNode("client", "client"));
        GraphNode service = graph.getOrCreateNode(new GraphNode("service", "service"));
        GraphNode database = graph.getOrCreateNode(new GraphNode("database", "database"));
        graph.addEntryNode(client);
        SynchronousCallEdge request = new SynchronousCallEdge(client, service);
        DatabaseQueryEdge query = new DatabaseQueryEdge(service, database, "find addresses by id");
        int requestRow = request.addData("trace", object("{billingId:'B',shippingId:'S'}"),
                object("{billingName:'Billing',shippingName:'Shipping'}"));
        query.addData("trace", object("{filter:{id:'UNRELATED'}}"), object("{name:'Unrelated'}"));
        int billingRow = query.addData("trace", object("{filter:{id:'B'}}"), object("{name:'Billing'}"));
        int shippingRow = query.addData("trace", object("{filter:{id:'S'}}"), object("{name:'Shipping'}"));
        graph.addCallRelationGroup(List.of(new CallRelation("trace", request, requestRow, query, billingRow)));
        graph.addCallRelationGroup(List.of(new CallRelation("trace", request, requestRow, query, shippingRow)));

        graph.finalizeGraph();

        assertEquals(2, request.getDataFlowsTo(query).size());
        DataFlow billing = flow(request, query, requestRow, billingRow);
        DataFlow shipping = flow(request, query, requestRow, shippingRow);
        assertMapping(billing, "input.billingId", "input.filter.id");
        assertMapping(shipping, "input.shippingId", "input.filter.id");
        assertFalse(billing.getDataFlows().containsKey(request.getSchema().findDataSchemaEntry("input.shippingId")));
        assertFalse(shipping.getDataFlows().containsKey(request.getSchema().findDataSchemaEntry("input.billingId")));
        assertMapping(flow(query, request, billingRow, requestRow), "output.name", "output.billingName");
        assertMapping(flow(query, request, shippingRow, requestRow), "output.name", "output.shippingName");

        DataPathNode billingPath = graph.deriveDataPaths("input.billingId").getRoots().getFirst();
        assertEquals(requestRow, billingPath.getRow());
        assertEquals(1, billingPath.getNext().size());
        assertEquals(billingRow, billingPath.getNext().getFirst().getRow());
        assertTrue(request.getDataByTrace().isEmpty());
        assertTrue(query.getDataByTrace().isEmpty());
        assertTrue(graph.getCallRelations().isEmpty());
        assertTrue(query.isFrozen());

        JSONObject exported = new JSONObject(GraphJsonExporter.export(graph));
        JSONArray flows = exported.getJSONArray("dataFlows");
        assertEquals(4, flows.length());
        assertTrue(hasRows(flows, requestRow, billingRow));
        assertTrue(hasRows(flows, requestRow, shippingRow));
        assertTrue(hasRows(flows, billingRow, requestRow));
        assertTrue(hasRows(flows, shippingRow, requestRow));
        String snapshot = GraphJsonExporter.export(graph);
        assertFalse(snapshot.contains("Unrelated"));
        graph.finalizeGraph();
        assertEquals(snapshot, GraphJsonExporter.export(graph));
    }

    @Test
    void pathsCannotJumpBetweenIdenticalValuesInDifferentOccurrencesOfASharedEdge() {
        Graph graph = new Graph();
        GraphNode first = graph.getOrCreateNode(new GraphNode("first", "first"));
        GraphNode second = graph.getOrCreateNode(new GraphNode("second", "second"));
        GraphNode third = graph.getOrCreateNode(new GraphNode("third", "third"));
        GraphNode left = graph.getOrCreateNode(new GraphNode("left", "left"));
        GraphNode right = graph.getOrCreateNode(new GraphNode("right", "right"));
        graph.addEntryNode(first);
        AsynchronousCallEdge start = new AsynchronousCallEdge(first, second);
        AsynchronousCallEdge shared = new AsynchronousCallEdge(second, third);
        AsynchronousCallEdge leftCall = new AsynchronousCallEdge(third, left);
        AsynchronousCallEdge rightCall = new AsynchronousCallEdge(third, right);
        start.addData("trace", object("{id:'same'}"), null);
        shared.addData("trace", object("{id:'same'}"), null);
        shared.addData("trace", object("{id:'same'}"), null);
        leftCall.addData("trace", object("{id:'same'}"), null);
        rightCall.addData("trace", object("{id:'same'}"), null);
        graph.addCallRelation(new CallRelation("trace", start, 0, shared, 0));
        graph.addCallRelation(new CallRelation("trace", shared, 0, leftCall, 0));
        graph.addCallRelation(new CallRelation("trace", shared, 1, rightCall, 0));

        graph.finalizeGraph();

        DataPaths paths = graph.deriveDataPaths("id");
        assertEquals(1, paths.getRoots().size());
        DataPathNode root = paths.getRoots().getFirst();
        assertEquals("trace", root.getTraceId());
        assertEquals(1, root.getNext().size());
        DataPathNode middle = root.getNext().getFirst();
        assertSame(shared, middle.getEdge());
        assertEquals(0, middle.getRow());
        assertEquals(1, middle.getNext().size());
        assertSame(leftCall, middle.getNext().getFirst().getEdge());
        assertEquals(0, middle.getNext().getFirst().getRow());
        assertEquals(2, shared.getDataFlows().size());
    }

    @Test
    void responseCanSupplyAnotherOccurrenceOfTheSameSynchronousEdge() {
        Graph graph = new Graph();
        GraphNode caller = graph.getOrCreateNode(new GraphNode("caller", "caller"));
        GraphNode database = graph.getOrCreateNode(new GraphNode("database", "database"));
        graph.addEntryNode(caller);
        DatabaseQueryEdge query = new DatabaseQueryEdge(caller, database, "GET next key");
        query.addData("trace", object("{key:'A'}"), object("{next:'B'}"));
        query.addData("trace", object("{key:'B'}"), object("{next:'C'}"));
        query.addData("trace", object("{key:'C'}"), object("{next:'D'}"));
        graph.addCallRelation(new CallRelation("trace", query, 0, query, 1));
        graph.addCallRelation(new CallRelation("trace", query, 1, query, 2));

        graph.finalizeGraph();

        assertEquals(2, query.getDataFlowsTo(query).size());
        assertMapping(flow(query, query, 0, 1), "output.next", "input.key");
        assertMapping(flow(query, query, 1, 2), "output.next", "input.key");
        List<DataPathNode> roots = graph.deriveDataPaths("output.next").getRoots();
        assertEquals(2, roots.size());
        assertEquals(List.of(0, 1), roots.stream().map(DataPathNode::getRow).toList());
        for (DataPathNode root : roots) {
            assertEquals(1, root.getNext().size());
            assertSame(query, root.getNext().getFirst().getEdge());
            assertEquals(root.getRow() + 1, root.getNext().getFirst().getRow());
        }
    }

    @Test
    void pathsFollowDifferentOccurrencesOfTheSameFieldAndStopAtAnActualCycle() {
        Graph graph = new Graph();
        GraphNode caller = graph.getOrCreateNode(new GraphNode("caller", "caller"));
        GraphNode topic = graph.getOrCreateNode(new GraphNode("topic", "topic"));
        graph.addEntryNode(caller);
        AsynchronousCallEdge edge = new AsynchronousCallEdge(caller, topic);
        for (int row = 0; row < 3; row++) {
            edge.addData("trace", object("{id:'same'}"), null);
        }
        graph.addCallRelation(new CallRelation("trace", edge, 0, edge, 1));
        graph.addCallRelation(new CallRelation("trace", edge, 1, edge, 2));
        graph.addCallRelation(new CallRelation("trace", edge, 2, edge, 0));

        graph.finalizeGraph();

        DataPathNode root = graph.deriveDataPaths("id").getRoots().stream()
                .filter(node -> Integer.valueOf(0).equals(node.getRow())).findFirst().orElseThrow();
        DataPathNode current = root;
        for (int expectedRow : List.of(0, 1, 2)) {
            assertEquals(expectedRow, current.getRow());
            assertSame(edge, current.getEdge());
            assertEquals(1, current.getNext().size());
            current = current.getNext().getFirst();
        }
        assertEquals(0, current.getRow());
        assertTrue(current.getNext().isEmpty());
    }

    private static DataFlow flow(CallEdge source, CallEdge target, int sourceRow, int targetRow) {
        return source.getDataFlowsTo(target).stream()
                .filter(flow -> Integer.valueOf(sourceRow).equals(flow.getSourceRow())
                        && Integer.valueOf(targetRow).equals(flow.getTargetRow()))
                .findFirst().orElseThrow();
    }

    private static void assertMapping(DataFlow flow, String sourcePath, String targetPath) {
        DataSchemaEntry<SimpleDataSchema> source = flow.getFrom().getSchema().findDataSchemaEntry(sourcePath);
        DataSchemaEntry<SimpleDataSchema> target = flow.getTo().getSchema().findDataSchemaEntry(targetPath);
        assertNotNull(source);
        assertNotNull(target);
        assertSame(target, flow.getDataFlows().get(source));
    }

    private static boolean hasRows(JSONArray flows, int sourceRow, int targetRow) {
        for (int index = 0; index < flows.length(); index++) {
            JSONObject flow = flows.getJSONObject(index);
            if (flow.getInt("sourceRow") == sourceRow && flow.getInt("targetRow") == targetRow) {
                assertEquals("trace", flow.getString("traceId"));
                return true;
            }
        }
        return false;
    }

    private static DataObject object(String json) {
        return DataObject.parseData(json);
    }
}
