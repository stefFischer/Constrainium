package at.sfischer.constraints.data.model;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.InOutputDataSchema;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.DefaultTypePromotionPolicy;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.data.model.utils.GraphJsonExporter;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.IntegerLiteral;
import at.sfischer.constraints.model.DataReference;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GraphFinalizationTest {

    @Test
    void preservesAlternativeFlowsForDifferentTraceIdsOnTheSameEdges() {
        Graph graph = graph();
        SynchronousCallEdge source = source(graph);
        SynchronousCallEdge target = target(graph);
        source.addData("shipping", data("{mode:'shipping'}", "{shipping:'A',billing:'B'}"));
        source.addData("billing", data("{mode:'billing'}", "{shipping:'A',billing:'B'}"));
        target.addData("shipping", data("{selected:'A'}", "{}"));
        target.addData("billing", data("{selected:'B'}", "{}"));
        graph.addCallRelation(new CallRelation("shipping", source, 0, target, 0));
        graph.addCallRelation(new CallRelation("billing", source, 0, target, 0));
        assertFalse(graph.isFinalized());
        assertEquals(2, source.getDataByTrace().size());
        assertTrue(source.getDataFlows().isEmpty());
        assertThrows(IllegalStateException.class, source::getSchema);
        assertThrows(IllegalStateException.class, () -> GraphJsonExporter.export(graph));

        graph.finalizeGraph();

        Map<String, DataFlow> flows = source.getDataFlowsTo(target).stream()
                .collect(Collectors.toMap(DataFlow::getTraceId, flow -> flow));
        assertEquals(Set.of("shipping", "billing"), flows.keySet());
        DataSchemaEntry<SimpleDataSchema> shipping = source.getSchema().getOutputSchema().findDataSchemaEntry("shipping");
        DataSchemaEntry<SimpleDataSchema> billing = source.getSchema().getOutputSchema().findDataSchemaEntry("billing");
        DataSchemaEntry<SimpleDataSchema> selected = target.getSchema().getInputSchema().findDataSchemaEntry("selected");
        assertSame(selected, flows.get("shipping").getDataFlows().get(shipping));
        assertFalse(flows.get("shipping").getDataFlows().containsKey(billing));
        assertSame(selected, flows.get("billing").getDataFlows().get(billing));
        assertFalse(flows.get("billing").getDataFlows().containsKey(shipping));
        assertEquals(1, source.getFrom().deriveDataPaths(shipping).getRoots().size());
        assertEquals(1, source.getFrom().deriveDataPaths(billing).getRoots().size());
        assertEquals(Set.of("shipping", "billing"), graph.getTraceIds());
        assertTrue(source.getDataByTrace().isEmpty());
        assertTrue(target.getDataByTrace().isEmpty());
        assertTrue(graph.getCallRelations().isEmpty());
        JSONObject document = new JSONObject(GraphJsonExporter.export(graph));
        assertTrue(document.getBoolean("finalized"));
        assertEquals(2, document.getJSONArray("traceIds").length());
    }

    @Test
    void derivesSchemasFromAllSamplesAndRetainsRequestResponsePairs() {
        Graph graph = graph();
        SynchronousCallEdge edge = source(graph);
        DataObject input = DataObject.parseData("{id:'A'}");
        InOutputDataCollection first = new InOutputDataCollection();
        first.addDataEntry(input, DataObject.parseData("{price:10,description:'hay'}"));
        edge.addData("trace-a", first);
        edge.addData("trace-b", data("{id:'B'}", "{price:12.5,stock:3}"));
        input.putValue("id", "CHANGED");
        Map<String, InOutputDataCollection> snapshot = edge.getDataByTrace();
        snapshot.get("trace-a").getInputDataCollection().getDataCollection().getFirst().putValue("id", "ALSO-CHANGED");
        snapshot.get("trace-b").clear();
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertEquals("A", edge.getDataByTrace().get("trace-a").getInputDataCollection()
                .getDataCollection().getFirst().getDataValue("id").getValue());
        assertEquals("B", edge.getDataByTrace().get("trace-b").getInputDataCollection()
                .getDataCollection().getFirst().getDataValue("id").getValue());

        graph.finalizeGraph();

        assertEquals(TypeEnum.NUMBER, edge.getSchema().getOutputSchema().findDataSchemaEntry("price").type);
        assertFalse(edge.getSchema().getOutputSchema().findDataSchemaEntry("description").mandatory);
        assertFalse(edge.getSchema().getOutputSchema().findDataSchemaEntry("stock").mandatory);
        assertTrue(edge.isFrozen());
        assertTrue(edge.getDataByTrace().isEmpty());
    }

    @Test
    void finalizationBlocksTopologyDataAndFlowMutationsIncludingHeldViews() {
        Graph graph = graph();
        SynchronousCallEdge source = source(graph);
        SynchronousCallEdge target = target(graph);
        source.addData("trace", data("{raw:'RAW_PAYLOAD_SENTINEL'}", "{id:'A'}"));
        target.addData("trace", data("{id:'A'}", "{}"));
        CallRelation relation = new CallRelation("trace", source, 0, target, 0);
        graph.addCallRelation(relation);
        Collection<GraphNode> nodes = graph.getNodes();
        Set<GraphNode> entries = graph.getEntryNodes();
        List<DataFlow> heldFlows = source.getDataFlows();

        graph.finalizeGraph();

        DataFlow flow = source.getDataFlowsTo(target).getFirst();
        DataSchemaEntry<SimpleDataSchema> field = source.getSchema().getOutputSchema().findDataSchemaEntry("id");
        assertThrows(IllegalStateException.class, () -> graph.getOrCreateNode(new GraphNode("new", "new")));
        assertThrows(IllegalStateException.class, () -> graph.addEntryNode(source.getFrom()));
        assertThrows(UnsupportedOperationException.class, nodes::clear);
        assertThrows(UnsupportedOperationException.class, entries::clear);
        assertThrows(UnsupportedOperationException.class, heldFlows::clear);
        assertThrows(IllegalStateException.class, () -> source.addData("trace", data("{}", "{}")));
        assertThrows(IllegalStateException.class, () -> graph.addCallRelation(relation));
        assertThrows(IllegalStateException.class, () -> source.addDataFlow(new DataFlow("late", source, target)));
        assertThrows(UnsupportedOperationException.class, () -> flow.getDataFlows().clear());
        assertThrows(IllegalStateException.class, () -> flow.addMapping(field, field));
        assertThrows(IllegalStateException.class, () -> source.getFrom().addOutgoing(source));
        GraphNode free = new GraphNode("free", "free");
        assertThrows(IllegalStateException.class, () -> new DependencyEdge(free, source.getFrom()));
        assertTrue(free.getOutgoing().isEmpty());
        String json = GraphJsonExporter.export(graph);
        graph.finalizeGraph();
        assertEquals(json, GraphJsonExporter.export(graph));
        assertFalse(json.contains("RAW_PAYLOAD_SENTINEL"));
    }

    @Test
    void finalizationLeavesSharedSchemaApisMutable() {
        Graph graph = graph();
        SimpleDataSchema input = new SimpleDataSchema();
        DataSchemaEntry<SimpleDataSchema> item = input.objectEntry("item", true);
        DataSchemaEntry<SimpleDataSchema> quantity = item.dataSchema.integerEntry("quantity", true);
        InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(input, new SimpleDataSchema());
        SynchronousCallEdge edge = new SynchronousCallEdge(graph.getNode("caller"), graph.getNode("source"), schema);

        graph.finalizeGraph();

        assertSame(schema, edge.getSchema());
        quantity.type = TypeEnum.NUMBER;
        Constraint constraint = new Constraint(new EqualOperator(new DataReference(quantity), new IntegerLiteral(1)));
        quantity.constraints.add(constraint);
        quantity.potentialConstraints.add(constraint);
        assertEquals(TypeEnum.NUMBER, quantity.type);
        assertTrue(quantity.constraints.contains(constraint));
        assertTrue(quantity.potentialConstraints.contains(constraint));
        quantity.constraints.clear();
        quantity.potentialConstraints.clear();
        assertTrue(quantity.constraints.isEmpty());
        assertTrue(quantity.potentialConstraints.isEmpty());

        DataSchemaEntry<SimpleDataSchema> description = item.dataSchema.stringEntry("description", false);
        assertSame(description, input.findDataSchemaEntry("item.description"));
        SimpleDataSchema extraOutput = new SimpleDataSchema();
        extraOutput.stringEntry("status", true);
        schema.unify(new InOutputDataSchema<>(new SimpleDataSchema(), extraOutput), null);
        assertNotNull(schema.getOutputSchema().findDataSchemaEntry("status"));
        assertTrue(graph.isFinalized());
        assertTrue(edge.isFrozen());
    }

    @Test
    void asynchronousEdgesDiscardResponsesWhenDerivingTheirSchema() {
        Graph graph = graph();
        AsynchronousCallEdge edge = new AsynchronousCallEdge(graph.getNode("caller"), graph.getNode("source"));
        edge.addData("trace", data("{orderId:'O1'}", "{ignored:'noise'}"));

        graph.finalizeGraph();

        assertEquals(Set.of("orderId"), edge.getSchema().getSchemaEntryNames());
        assertTrue(edge.getDataByTrace().isEmpty());
    }

    @Test
    void rejectsMissingTraceGroupsAndInvalidRowReferencesWithoutLosingData() {
        Graph graph = graph();
        SynchronousCallEdge source = source(graph);
        SynchronousCallEdge target = target(graph);
        source.addData("one", data("{}", "{id:'A'}"));
        target.addData("two", data("{id:'A'}", "{}"));

        assertThrows(IllegalArgumentException.class, () ->
                graph.addCallRelation(new CallRelation("one", source, 0, target, 0)));
        target.addData("one", data("{id:'A'}", "{}"));
        assertThrows(IllegalArgumentException.class, () ->
                graph.addCallRelation(new CallRelation("one", source, -1, target, 0)));
        assertThrows(IllegalArgumentException.class, () ->
                graph.addCallRelation(new CallRelation("one", source, 1, target, 0)));
        assertThrows(IllegalArgumentException.class, () ->
                graph.addCallRelation(new CallRelation("one", source, 0, target, 1)));
        assertEquals(1, source.getDataByTrace().size());
        assertTrue(graph.getCallRelations().isEmpty());
    }

    @Test
    void failedDerivationKeepsTheGraphMutableAndPreservesItsEvidence() {
        Graph graph = graph();
        SynchronousCallEdge edge = source(graph);
        edge.addData("one", data("{value:1}", "{}"));
        edge.addData("two", data("{value:'two'}", "{}"));

        assertThrows(IllegalStateException.class, graph::finalizeGraph);

        assertFalse(graph.isFinalized());
        assertFalse(edge.isFrozen());
        assertEquals(2, edge.getDataByTrace().size());
        graph.getOrCreateNode(new GraphNode("extra", "extra"));
        graph.finalizeGraph(new DefaultTypePromotionPolicy());
        assertTrue(graph.isFinalized());
        assertTrue(edge.getDataByTrace().isEmpty());
    }

    @Test
    void appendingBatchesPreservesExistingRowReferencesAndOnlyRelatedRowsAreCompared() {
        Graph graph = graph();
        SynchronousCallEdge cart = source(graph);
        SynchronousCallEdge products = target(graph);
        int cartRow = cart.addData("trace", data("{}", "{ids:['A','B']}"));
        InOutputDataCollection batch = data("{id:'A'}", "{echo:'A'}");
        batch.addDataEntry(DataObject.parseData("{id:'B'}"), DataObject.parseData("{echo:'B'}"));
        int productStart = products.addData("trace", batch);
        assertEquals(0, productStart);
        graph.addCallRelation(new CallRelation("trace", cart, cartRow, products, productStart));
        graph.addCallRelation(new CallRelation("trace", cart, cartRow, products, productStart + 1));
        graph.addCallRelation(new CallRelation("trace", cart, cartRow, products, productStart + 1));
        assertEquals(2, graph.getCallRelations().size());
        assertEquals(2, products.addData("trace", data("{id:'UNRELATED'}", "{echo:'UNRELATED'}")));
        InOutputDataCollection stored = products.getDataByTrace().get("trace");
        assertEquals(3, stored.size());
        assertEquals("A", stored.getInputDataCollection().getDataCollection().getFirst().getDataValue("id").getValue());
        assertEquals("A", stored.getOutputDataCollection().getDataCollection().getFirst().getDataValue("echo").getValue());
        assertEquals("B", stored.getInputDataCollection().getDataCollection().get(1).getDataValue("id").getValue());
        assertEquals("B", stored.getOutputDataCollection().getDataCollection().get(1).getDataValue("echo").getValue());

        graph.finalizeGraph();

        assertEquals(1, cart.getDataFlowsTo(products).size());
        DataSchemaEntry<SimpleDataSchema> source = cart.getSchema().getOutputSchema().findDataSchemaEntry("ids");
        DataSchemaEntry<SimpleDataSchema> target = products.getSchema().getInputSchema().findDataSchemaEntry("id");
        assertNotNull(source);
        assertNotNull(target);
        assertSame(target, cart.getDataFlowsTo(products).getFirst().getDataFlows().get(source));
    }

    @Test
    void identicalRowsRemainDistinctWhenAppendedToACollection() {
        Graph graph = graph();
        SynchronousCallEdge edge = source(graph);
        InOutputDataCollection row = data("{id:'A'}", "{result:'OK'}");

        assertEquals(0, edge.addData("trace", row));
        assertEquals(1, edge.addData("trace", row));
        assertEquals(2, edge.getDataByTrace().get("trace").size());
        assertEquals(0, edge.addData("other-trace", row));
        assertEquals(Set.of("trace", "other-trace"), edge.getTraceIds());
    }

    private static Graph graph() {
        Graph graph = new Graph();
        graph.addEntryNode(new GraphNode("caller", "caller"));
        graph.getOrCreateNode(new GraphNode("source", "source"));
        graph.getOrCreateNode(new GraphNode("target", "target"));
        return graph;
    }

    private static SynchronousCallEdge source(Graph graph) {
        return new SynchronousCallEdge(graph.getNode("caller"), graph.getNode("source"));
    }

    private static SynchronousCallEdge target(Graph graph) {
        return new SynchronousCallEdge(graph.getNode("caller"), graph.getNode("target"));
    }

    private static InOutputDataCollection data(String input, String output) {
        InOutputDataCollection data = new InOutputDataCollection();
        data.addDataEntry(DataObject.parseData(input), DataObject.parseData(output));
        return data;
    }
}
