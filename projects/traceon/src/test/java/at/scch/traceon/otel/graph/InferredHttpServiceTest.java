package at.scch.traceon.otel.graph;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.data.model.CallEdge;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.constraints.data.model.GraphNode;
import at.sfischer.constraints.data.model.SynchronousCallEdge;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;
import at.sfischer.traces.otel.dataextraction.SpanData;
import at.sfischer.traces.otel.graph.CallGraphBuilder;
import at.sfischer.traces.otel.graph.DispatchingSpanFragmentBuilder;
import at.sfischer.traces.otel.graph.GraphFragment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InferredHttpServiceTest {

    @Test
    void accountingFragmentOwnsTheInferredServiceBeforeFinalization() {
        Graph graph = new Graph();
        GraphFragment fragment = new TraceonSpanFragmentBuilders.HttpClient()
                .buildFragment(new ClusteredAbstractSpan(accounting()), graph);

        assertEquals("endpoint", fragment.entryNode().getType());
        assertEquals("accounting-interface", fragment.exitNode().getName());
        assertEquals("service", fragment.exitNode().getType());
        assertEquals(1, fragment.internalEdges().size());
        SynchronousCallEdge processing = (SynchronousCallEdge) fragment.internalEdges().getFirst();
        assertSame(fragment.entryNode(), processing.getFrom());
        assertSame(fragment.exitNode(), processing.getTo());
        assertThrows(IllegalStateException.class, processing::getSchema);
    }

    @Test
    void assignsAccountingAndJwksSchemasToTheirRespectiveEdges() {
        Graph graph = graph();
        SynchronousCallEdge request = accountingRequest(graph);
        SynchronousCallEdge processing = accountingProcessing(graph);
        SynchronousCallEdge jwks = jwksRequest(graph);

        assertAccountingSchema(request);
        assertAccountingSchema(processing);
        assertNotNull(jwks.getSchema().getInputSchema().findDataSchemaEntry("query_params.selectedOrder"));
        assertNull(jwks.getSchema().getInputSchema().findDataSchemaEntry("payload.orderId"));
        assertNotNull(jwks.getSchema().getOutputSchema().findDataSchemaEntry("payload.keys.kid"));
        assertNull(jwks.getSchema().getOutputSchema().findDataSchemaEntry("payload.receiptKey"));
        assertEquals("endpoint", request.getTo().getType());
        assertEquals("service", processing.getTo().getType());
        assertTrue(graph.getNodes().stream().flatMap(node -> node.getOutgoing().stream())
                .noneMatch(edge -> edge.getFrom() == edge.getTo()));
    }

    @Test
    void infersRequestAndResponseFlowsAcrossTheInferredService() {
        Graph graph = graph();
        SynchronousCallEdge request = accountingRequest(graph);
        SynchronousCallEdge processing = accountingProcessing(graph);
        SynchronousCallEdge jwks = jwksRequest(graph);

        assertMapping(request, processing,
                request.getSchema().getInputSchema().findDataSchemaEntry("payload.orderId"),
                processing.getSchema().getInputSchema().findDataSchemaEntry("payload.orderId"));
        assertMapping(processing, request,
                processing.getSchema().getOutputSchema().findDataSchemaEntry("payload.receiptKey"),
                request.getSchema().getOutputSchema().findDataSchemaEntry("payload.receiptKey"));
        assertMapping(processing, jwks,
                processing.getSchema().getInputSchema().findDataSchemaEntry("payload.orderId"),
                jwks.getSchema().getInputSchema().findDataSchemaEntry("query_params.selectedOrder"));
        assertMapping(jwks, processing,
                jwks.getSchema().getOutputSchema().findDataSchemaEntry("payload.keys.kid"),
                processing.getSchema().getOutputSchema().findDataSchemaEntry("payload.receiptKey"));
    }

    @Test
    void missingJwksRequestDataDoesNotReplaceTheAccountingPayload() {
        AbstractSpan accounting = accounting();
        accounting.getChildren().getFirst().setSpanData(new SpanData(
                null, DataObject.parseData("{payload:{keys:[{kid:'KEY-1'}]}}")));
        Graph graph = graph(accounting);
        SynchronousCallEdge request = accountingRequest(graph);
        SynchronousCallEdge processing = accountingProcessing(graph);
        SynchronousCallEdge jwks = jwksRequest(graph);

        assertAccountingSchema(request);
        assertAccountingSchema(processing);
        assertTrue(jwks.getSchema().getInputSchema().getSchemaEntryNames().isEmpty());
        assertMapping(request, processing,
                request.getSchema().getInputSchema().findDataSchemaEntry("payload.orderId"),
                processing.getSchema().getInputSchema().findDataSchemaEntry("payload.orderId"));
        assertTrue(processing.getDataFlowsTo(jwks).stream().allMatch(flow -> flow.getDataFlows().isEmpty()));
        assertMapping(jwks, processing,
                jwks.getSchema().getOutputSchema().findDataSchemaEntry("payload.keys.kid"),
                processing.getSchema().getOutputSchema().findDataSchemaEntry("payload.receiptKey"));
    }

    @Test
    void doesNotInferADestinationFromChildrenInTheCallersService() {
        AbstractSpan client = http("client", "order-service", "POST", "/accounting/orders", "{}", "{}");
        client.addChild(http("client", "order-service", "GET", "/local", "{}", "{}"));
        Graph graph = new Graph();

        GraphFragment fragment = new TraceonSpanFragmentBuilders.HttpClient()
                .buildFragment(new ClusteredAbstractSpan(client), graph);

        assertSame(fragment.entryNode(), fragment.exitNode());
        assertTrue(fragment.internalEdges().isEmpty());
        assertEquals(1, graph.getNodes().size());
    }

    @Test
    void keepsTheEndpointOnlyWhenChildServicesAreAmbiguous() {
        AbstractSpan client = accounting();
        client.addChild(http("client", "another-service", "GET", "/other", "{}", "{}"));

        GraphFragment fragment = new TraceonSpanFragmentBuilders.HttpClient()
                .buildFragment(new ClusteredAbstractSpan(client), new Graph());

        assertSame(fragment.entryNode(), fragment.exitNode());
        assertTrue(fragment.internalEdges().isEmpty());
    }

    @Test
    void anObservedServerTakesPrecedenceOverInferredChildServices() {
        AbstractSpan client = accounting();
        client.addChild(http("server", "observed-service", "POST", "/accounting/orders", "{}", "{}"));
        ClusteredAbstractSpan span = new ClusteredAbstractSpan(client);
        Graph graph = new Graph();

        GraphFragment clientFragment = new TraceonSpanFragmentBuilders.HttpClient().buildFragment(span, graph);
        GraphFragment serverFragment = new TraceonSpanFragmentBuilders.HttpServer()
                .buildFragment(span.getChildren().getLast(), graph);

        assertSame(clientFragment.entryNode(), serverFragment.entryNode());
        assertSame(clientFragment.entryNode(), clientFragment.exitNode());
        assertTrue(clientFragment.internalEdges().isEmpty());
        assertFalse(graph.getNodes().stream().anyMatch(node -> node.getName().equals("accounting-interface")));
    }

    private static Graph graph() {
        return graph(accounting());
    }

    private static Graph graph(AbstractSpan accounting) {
        AbstractSpan root = http("server", "order-service", "POST", "/orders",
                "{sourceOrderRef:'ORDER-1'}", "{finalKey:'KEY-1'}");
        root.addChild(accounting);
        CallGraphBuilder builder = new CallGraphBuilder(new DispatchingSpanFragmentBuilder(
                new TraceonSpanFragmentBuilders.HttpServer(), new TraceonSpanFragmentBuilders.HttpClient()));
        return builder.build(new ClusteredAbstractSpan(root), new TraceonTypePromotionPolicy());
    }

    private static AbstractSpan accounting() {
        AbstractSpan client = http("client", "order-service", "POST", "/accounting/orders",
                "{payload:{orderId:'ORDER-1',totalPrice:42}}", "{payload:{receiptKey:'KEY-1'}}");
        client.addChild(http("client", "accounting-interface", "GET", "/oauth2/jwks",
                "{query_params:{selectedOrder:'ORDER-1'}}", "{payload:{keys:[{kid:'OTHER'},{kid:'KEY-1'}]}}"));
        return client;
    }

    private static AbstractSpan http(String kind, String service, String method, String route,
                                     String input, String output) {
        AbstractSpan span = new AbstractSpan(method + " " + route, kind);
        span.getAttributes().put("service.name", service);
        span.getAttributes().put("http.request.method", method);
        span.getAttributes().put("http.route", route);
        span.setSpanData(new SpanData(DataObject.parseData(input), DataObject.parseData(output)));
        return span;
    }

    private static SynchronousCallEdge accountingRequest(Graph graph) {
        GraphNode caller = node(graph, "order-service");
        return (SynchronousCallEdge) caller.getOutgoing().getFirst();
    }

    private static SynchronousCallEdge accountingProcessing(Graph graph) {
        return (SynchronousCallEdge) node(graph, "accounting-interface").getIncoming().getFirst();
    }

    private static SynchronousCallEdge jwksRequest(Graph graph) {
        return (SynchronousCallEdge) node(graph, "accounting-interface").getOutgoing().getFirst();
    }

    private static GraphNode node(Graph graph, String name) {
        return graph.getNodes().stream().filter(node -> node.getName().equals(name)).findFirst().orElseThrow();
    }

    private static void assertAccountingSchema(SynchronousCallEdge edge) {
        assertNotNull(edge.getSchema().getInputSchema().findDataSchemaEntry("payload.orderId"));
        assertNotNull(edge.getSchema().getInputSchema().findDataSchemaEntry("payload.totalPrice"));
        assertNull(edge.getSchema().getInputSchema().findDataSchemaEntry("query_params.selectedOrder"));
        assertNotNull(edge.getSchema().getOutputSchema().findDataSchemaEntry("payload.receiptKey"));
        assertNull(edge.getSchema().getOutputSchema().findDataSchemaEntry("payload.keys"));
    }

    private static void assertMapping(CallEdge source, CallEdge target,
                                       DataSchemaEntry<SimpleDataSchema> sourceField,
                                       DataSchemaEntry<SimpleDataSchema> targetField) {
        assertNotNull(sourceField);
        assertNotNull(targetField);
        assertTrue(source.getDataFlowsTo(target).stream()
                .anyMatch(flow -> flow.getDataFlows().get(sourceField) == targetField),
                "Missing flow from " + sourceField.getQualifiedName() + " to " + targetField.getQualifiedName());
    }
}
