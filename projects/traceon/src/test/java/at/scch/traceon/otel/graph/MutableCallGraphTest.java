package at.scch.traceon.otel.graph;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.data.model.CallEdge;
import at.sfischer.constraints.data.model.DataFlow;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.constraints.data.model.GraphNode;
import at.sfischer.constraints.data.model.SynchronousCallEdge;
import at.sfischer.constraints.data.model.utils.GraphJsonExporter;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;
import at.sfischer.traces.otel.dataextraction.SpanData;
import at.sfischer.traces.otel.graph.CallGraphBuilder;
import at.sfischer.traces.otel.graph.DispatchingSpanFragmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MutableCallGraphTest {

    @Test
    void repeatedProductCallsShareEdgesAndKeepAllEvidenceUntilFinalization() {
        ClusteredAbstractSpan cluster = new ClusteredAbstractSpan(checkout("A", "B"));
        Graph graph = builder().buildMutable(cluster, List.of("checkout-one"));
        GraphNode caller = node(graph, "checkout-service");
        List<CallEdge> outgoing = caller.getOutgoing().stream().map(CallEdge.class::cast).toList();
        SynchronousCallEdge cart = (SynchronousCallEdge) outgoing.getFirst();
        SynchronousCallEdge products = (SynchronousCallEdge) outgoing.getLast();
        SynchronousCallEdge processing = (SynchronousCallEdge) node(graph, "product-service").getIncoming().getFirst();
        assertEquals(2, outgoing.size());
        assertEquals(1, node(graph, "product-service").getIncoming().size());
        assertEquals(2, rowCount(products));
        assertEquals(2, rowCount(processing));
        assertFalse(graph.isFinalized());
        assertFalse(graph.getCallRelations().isEmpty());
        assertTrue(cart.getDataFlows().isEmpty());
        assertThrows(IllegalStateException.class, products::getSchema);

        graph.finalizeGraph(new TraceonTypePromotionPolicy());

        assertTrue(graph.isFinalized());
        assertEquals(TypeEnum.NUMBER, products.getSchema().getOutputSchema().findDataSchemaEntry("payload.price").type);
        assertFalse(products.getSchema().getOutputSchema().findDataSchemaEntry("payload.description").mandatory);
        assertFalse(products.getSchema().getOutputSchema().findDataSchemaEntry("payload.stock").mandatory);
        assertMapping(cart, products, cart.getSchema().getOutputSchema().findDataSchemaEntry("payload.items.id"),
                products.getSchema().getInputSchema().findDataSchemaEntry("path_params.productId"));
        assertMapping(products, processing, products.getSchema().getInputSchema().findDataSchemaEntry("path_params.productId"),
                processing.getSchema().getInputSchema().findDataSchemaEntry("path_params.productId"));
        assertEquals("checkout-one", cart.getDataFlowsTo(products).getFirst().getTraceId());
        assertTrue(products.getDataByTrace().isEmpty());
        assertTrue(graph.getCallRelations().isEmpty());
        assertDoesNotThrow(() -> GraphJsonExporter.export(graph));
    }

    @Test
    void clusteredExecutionsKeepIndependentTraceIdsAndOriginalPairing() {
        ClusteredAbstractSpan cluster = new ClusteredAbstractSpan(checkout("A1", "A2"));
        cluster.addSpan(checkout("B1", "B2"));
        CallGraphBuilder builder = builder();
        Graph graph = builder.buildMutable(cluster, List.of("execution-a", "execution-b"));
        List<CallEdge> outgoing = node(graph, "checkout-service").getOutgoing().stream().map(CallEdge.class::cast).toList();
        SynchronousCallEdge cart = (SynchronousCallEdge) outgoing.getFirst();
        SynchronousCallEdge products = (SynchronousCallEdge) outgoing.getLast();
        assertEquals(4, rowCount(products));
        assertEquals(Set.of("execution-a", "execution-b"), products.getDataByTrace().keySet());

        graph.finalizeGraph(new TraceonTypePromotionPolicy());

        assertEquals(4, cart.getDataFlowsTo(products).size());
        assertEquals(Set.of("execution-a", "execution-b"), cart.getDataFlowsTo(products).stream()
                .map(flow -> flow.getTraceId()).collect(Collectors.toSet()));
        DataSchemaEntry<SimpleDataSchema> source = cart.getSchema().getOutputSchema().findDataSchemaEntry("payload.items.id");
        DataSchemaEntry<SimpleDataSchema> target = products.getSchema().getInputSchema().findDataSchemaEntry("path_params.productId");
        assertTrue(cart.getDataFlowsTo(products).stream().allMatch(flow -> flow.getDataFlows().get(source) == target));
        Graph rebuilt = builder.buildMutable(cluster, List.of("execution-a", "execution-b"));
        CallEdge rebuiltProducts = (CallEdge) node(rebuilt, "checkout-service").getOutgoing().getLast();
        assertEquals(4, rowCount(rebuiltProducts));
        assertEquals(2, cluster.getChildren().getFirst().getSpanData().size());
    }

    @Test
    void repeatedCallsWithDifferentMethodsOrDestinationsRemainSeparate() {
        AbstractSpan root = checkout("A", "B");
        root.addChild(http("client", "checkout-service", "DELETE", "/products/{productId}", "{}", "{}"));
        AbstractSpan other = http("client", "checkout-service", "GET", "/products/{productId}", "{}", "{}");
        other.getAttributes().put("server.address", "other-service");
        root.addChild(other);

        Graph graph = builder().buildMutable(new ClusteredAbstractSpan(root));

        assertEquals(4, node(graph, "checkout-service").getOutgoing().size());
    }

    @Test
    void missingCapturesPreserveTheirExecutionSlot() {
        AbstractSpan first = checkout("A1", "A2");
        AbstractSpan second = checkout("B1", "B2");
        second.getChildren().getLast().setSpanData(null);
        ClusteredAbstractSpan cluster = new ClusteredAbstractSpan(first);
        cluster.addSpan(second);

        Graph graph = builder().buildMutable(cluster, List.of("execution-a", "execution-b"));
        CallEdge products = (CallEdge) node(graph, "checkout-service").getOutgoing().getLast();

        assertEquals(4, rowCount(products));
        assertEquals(2, cluster.getChildren().getLast().getSpanData().size());
        assertDoesNotThrow(() -> graph.finalizeGraph(new TraceonTypePromotionPolicy()));
    }

    @Test
    void executionsSharingATraceGroupKeepTheirCollectionRowsCorrectlyPaired() {
        ClusteredAbstractSpan cluster = new ClusteredAbstractSpan(checkout("A1", "A2"));
        cluster.addSpan(checkout("B1", "B2"));
        Graph graph = builder().buildMutable(cluster, List.of("same-situation", "same-situation"));
        List<CallEdge> outgoing = node(graph, "checkout-service").getOutgoing().stream().map(CallEdge.class::cast).toList();
        SynchronousCallEdge cart = (SynchronousCallEdge) outgoing.getFirst();
        SynchronousCallEdge products = (SynchronousCallEdge) outgoing.getLast();
        assertEquals(2, cart.getDataByTrace().get("same-situation").size());
        assertEquals(4, products.getDataByTrace().get("same-situation").size());
        Set<String> pairings = graph.getCallRelations().stream()
                .filter(relation -> relation.source() == cart && relation.target() == products)
                .map(relation -> relation.sourceRow() + ":" + relation.targetRow()).collect(Collectors.toSet());
        assertEquals(Set.of("0:0", "1:1", "0:2", "1:3"), pairings);

        graph.finalizeGraph(new TraceonTypePromotionPolicy());

        assertEquals(4, cart.getDataFlowsTo(products).size());
        assertEquals("same-situation", cart.getDataFlowsTo(products).getFirst().getTraceId());
        assertMapping(cart, products, cart.getSchema().getOutputSchema().findDataSchemaEntry("payload.items.id"),
                products.getSchema().getInputSchema().findDataSchemaEntry("path_params.productId"));
        assertEquals(Set.of("0:0", "1:1", "0:2", "1:3"), cart.getDataFlowsTo(products).stream()
                .map(flow -> flow.getSourceRow() + ":" + flow.getTargetRow()).collect(Collectors.toSet()));
    }

    @Test
    void aCounterexampleInOneProductLookupDoesNotInvalidateTheOtherLookupOnTheSharedEdge() {
        AbstractSpan first = checkout("A1", "A2");
        AbstractSpan second = checkout("B1", "B2");
        AbstractSpan firstProduct = second.getChildren().get(1);
        firstProduct.setSpanData(new SpanData(DataObject.parseData("{path_params:{productId:'MISSING'}}"),
                firstProduct.getSpanData().getOutputData()));
        ClusteredAbstractSpan cluster = new ClusteredAbstractSpan(first);
        cluster.addSpan(second);
        Graph graph = builder().buildMutable(cluster, List.of("execution-a", "execution-b"));
        List<CallEdge> outgoing = node(graph, "checkout-service").getOutgoing().stream()
                .map(CallEdge.class::cast).toList();
        CallEdge cart = outgoing.getFirst();
        CallEdge products = outgoing.getLast();

        graph.finalizeGraph(new TraceonTypePromotionPolicy());

        DataSchemaEntry<SimpleDataSchema> ids = cart.getSchema().findDataSchemaEntry("output.payload.items.id");
        DataSchemaEntry<SimpleDataSchema> productId = products.getSchema().findDataSchemaEntry("input.path_params.productId");
        assertEquals(4, cart.getDataFlowsTo(products).size());
        for (DataFlow flow : cart.getDataFlowsTo(products)) {
            if (Integer.valueOf(0).equals(flow.getTargetRow())) {
                assertFalse(flow.getDataFlows().containsKey(ids));
            } else {
                assertEquals(1, flow.getTargetRow());
                assertSame(productId, flow.getDataFlows().get(ids));
            }
        }
    }

    @Test
    void repeatedHttpCallCanConsumeTheEarlierResponseOnTheSameEdge() {
        AbstractSpan root = http("server", "checkout-service", "POST", "/follow", "{}", "{}");
        AbstractSpan first = http("client", "checkout-service", "GET", "/next",
                "{key:'A'}", "{nextKey:'B'}");
        AbstractSpan second = http("client", "checkout-service", "GET", "/next",
                "{key:'B'}", "{nextKey:'C'}");
        first.getAttributes().put("server.address", "links-service");
        second.getAttributes().put("server.address", "links-service");
        root.addChild(first);
        root.addChild(second);
        Graph graph = builder().buildMutable(new ClusteredAbstractSpan(root), List.of("follow"));
        GraphNode caller = node(graph, "checkout-service");
        assertEquals(1, caller.getOutgoing().size());
        CallEdge edge = (CallEdge) caller.getOutgoing().getFirst();
        assertEquals(1, graph.getCallRelations().stream()
                .filter(relation -> relation.source() == edge && relation.target() == edge).count());
        assertTrue(graph.getCallRelations().stream()
                .noneMatch(relation -> relation.source() == relation.target()
                        && relation.sourceRow() == relation.targetRow()));

        graph.finalizeGraph();

        assertEquals(1, edge.getDataFlowsTo(edge).size());
        DataFlow flow = edge.getDataFlowsTo(edge).getFirst();
        assertEquals(0, flow.getSourceRow());
        assertEquals(1, flow.getTargetRow());
        assertMapping(edge, edge, edge.getSchema().findDataSchemaEntry("output.nextKey"),
                edge.getSchema().findDataSchemaEntry("input.key"));
        assertTrue(edge.getDataByTrace().isEmpty());
    }

    private static AbstractSpan checkout(String first, String second) {
        AbstractSpan root = http("server", "checkout-service", "POST", "/checkout", "{}", "{}");
        root.addChild(http("client", "checkout-service", "GET", "/carts/{cartId}", "{}",
                "{payload:{items:[{id:'" + first + "'},{id:'" + second + "'}]}}"));
        root.addChild(product(first, "{payload:{id:'" + first + "',price:10,description:'Hay'}}"));
        root.addChild(product(second, "{payload:{id:'" + second + "',price:12.5,stock:3}}"));
        return root;
    }

    private static AbstractSpan product(String id, String output) {
        String input = "{path_params:{productId:'" + id + "'}}";
        AbstractSpan client = http("client", "checkout-service", "GET", "/products/{productId}", input, output);
        client.getAttributes().put("server.address", "product-service");
        client.addChild(http("server", "product-service", "GET", "/products/{productId}", input, output));
        return client;
    }

    private static AbstractSpan http(String kind, String service, String method, String route, String input, String output) {
        AbstractSpan span = new AbstractSpan(method + " " + route, kind);
        span.getAttributes().put("service.name", service);
        span.getAttributes().put("http.request.method", method);
        span.getAttributes().put("http.route", route);
        span.setSpanData(new SpanData(DataObject.parseData(input), DataObject.parseData(output)));
        return span;
    }

    private static CallGraphBuilder builder() {
        return new CallGraphBuilder(new DispatchingSpanFragmentBuilder(
                new TraceonSpanFragmentBuilders.HttpServer(), new TraceonSpanFragmentBuilders.HttpClient()));
    }

    private static GraphNode node(Graph graph, String name) {
        return graph.getNodes().stream().filter(node -> node.getName().equals(name)).findFirst().orElseThrow();
    }

    private static int rowCount(CallEdge edge) {
        return edge.getDataByTrace().values().stream().mapToInt(collection -> collection.size()).sum();
    }

    private static void assertMapping(CallEdge source, CallEdge target,
                                       DataSchemaEntry<SimpleDataSchema> from, DataSchemaEntry<SimpleDataSchema> to) {
        assertNotNull(from);
        assertNotNull(to);
        assertTrue(source.getDataFlowsTo(target).stream().anyMatch(flow -> flow.getDataFlows().get(from) == to));
    }
}
