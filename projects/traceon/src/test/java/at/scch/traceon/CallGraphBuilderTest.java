package at.scch.traceon;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.model.*;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;
import at.sfischer.traces.otel.dataextraction.SpanData;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CallGraphBuilderTest {
    @Test
    void normalizesHttpNamesAndRoutesWithoutChangingOriginalSpans() {
        for (String kind : List.of("server", "client")) {
            var source = new at.sfischer.traces.otel.Span("GET /products/HAY-002", "span", "trace", null,
                    kind, "product-service", 0, 1);
            source.getAttributes().put("http.request.method", "GET");
            source.getAttributes().put("url.path", "/products/HAY-002");
            AbstractSpan result = Main.createHttpAbstractSpan(source);
            assertEquals("GET /products/{productId}", result.getName());
            assertEquals("/products/{productId}", result.getAttribute("http.route"));
            assertEquals(kind, result.getKind());
            assertEquals("GET /products/HAY-002", source.getName());
            assertEquals("/products/HAY-002", source.getAttribute("url.path"));
        }
    }

    @Test
    void buildsReachableGraphForAllTraceonSpanTypes() {
        AbstractSpan root = http("GET /checkout", "server", "checkout-service");
        AbstractSpan client = http("GET /products", "client", "checkout-service");
        AbstractSpan server = http("GET /products", "server", "product-service");
        root.addChild(client); client.addChild(server);
        server.addChild(span("find", "MONGO", "product-service"));
        root.addChild(span("redis pipeline", "REDIS", "checkout-service"));
        AbstractSpan producer = kafka("publish", "producer", "checkout-service");
        AbstractSpan consumer = kafka("consume", "consumer", "order-service");
        root.addChild(producer); producer.addChild(consumer);

        Graph graph = Main.CALL_GRAPH_BUILDER.build(new ClusteredAbstractSpan(root));
        Set<GraphNode> reachable = new HashSet<>();
        Deque<GraphNode> pending = new ArrayDeque<>(graph.getEntryNodes());
        while (!pending.isEmpty()) {
            GraphNode node = pending.remove();
            if (reachable.add(node)) node.getOutgoing().forEach(edge -> pending.add(edge.getTo()));
        }
        assertEquals(new HashSet<>(graph.getNodes()), reachable);
        List<Edge> edges = graph.getNodes().stream().flatMap(node -> node.getOutgoing().stream()).toList();
        assertTrue(edges.stream().anyMatch(SynchronousCallEdge.class::isInstance));
        assertEquals(2, edges.stream().filter(AsynchronousCallEdge.class::isInstance).count());
        assertTrue(edges.stream().allMatch(edge -> edge.getFrom() != edge.getTo()));
        assertTrue(edges.stream().map(CallEdge.class::cast).allMatch(edge -> edge.getSchema() != null));
    }

    @Test
    void mergesHttpClientWithObservedServerEndpoint() {
        AbstractSpan root = http("POST /checkout/{cartId}/pay", "server", "checkout-service");
        root.getAttributes().put("http.request.method", "POST");
        AbstractSpan client = http("GET /carts/123", "client", "checkout-service");
        client.getAttributes().put("server.address", "cart.internal");
        client.getAttributes().put("server.port", 8080);
        AbstractSpan server = http("GET /carts/{cartId}", "server", "cart-service");
        root.addChild(client);
        client.addChild(server);

        Graph graph = Main.CALL_GRAPH_BUILDER.build(new ClusteredAbstractSpan(root));
        assertEquals(4, graph.getNodes().size());
        GraphNode checkout = graph.getNodes().stream()
                .filter(node -> node.getName().equals("checkout-service")).findFirst().orElseThrow();
        GraphNode endpoint = checkout.getOutgoing().getFirst().getTo();
        assertEquals("cart-service / GET /carts/{cartId}", endpoint.getName());
        assertEquals("cart-service", endpoint.getOutgoing().getFirst().getTo().getName());
        assertTrue(graph.getNodes().stream().flatMap(node -> node.getOutgoing().stream())
                .allMatch(edge -> edge.getFrom() != edge.getTo()));
    }

    @Test
    void preservesHttpClientWithoutObservedServer() {
        AbstractSpan root = http("GET /checkout", "server", "checkout-service");
        AbstractSpan client = http("GET /carts", "client", "checkout-service");
        client.getAttributes().put("server.address", "cart-service");
        client.getAttributes().put("server.port", 8080);
        root.addChild(client);

        Graph graph = Main.CALL_GRAPH_BUILDER.build(new ClusteredAbstractSpan(root));
        assertEquals(3, graph.getNodes().size());
        assertTrue(graph.getNodes().stream().anyMatch(node ->
                node.getName().equals("cart-service / GET /carts")));
    }

    @Test
    void doesNotMergeIdenticallyNamedEndpointsAcrossServices() {
        AbstractSpan root = http("GET /health", "server", "first-service");
        root.addChild(http("GET /health", "server", "second-service"));
        Graph graph = Main.CALL_GRAPH_BUILDER.build(new ClusteredAbstractSpan(root));
        assertEquals(4, graph.getNodes().size());
    }

    @Test
    void supportsConsumerAndDatabaseRoots() {
        Graph consumer = Main.CALL_GRAPH_BUILDER.build(new ClusteredAbstractSpan(kafka("consume", "consumer", "order-service")));
        assertEquals(2, consumer.getNodes().size());
        assertTrue(consumer.getEntryNodes().iterator().next().getOutgoing().getFirst() instanceof AsynchronousCallEdge);
        Graph database = Main.CALL_GRAPH_BUILDER.build(new ClusteredAbstractSpan(span("GET", "REDIS", "cart-service")));
        assertEquals(1, database.getEntryNodes().size());
    }

    private AbstractSpan span(String name, String kind, String service) {
        AbstractSpan span = new AbstractSpan(name, kind);
        span.getAttributes().put("service.name", service);
        span.setSpanData(new SpanData(DataObject.parseData("{payload:{id:'123'}}"), DataObject.parseData("{payload:{id:'123'}}")));
        return span;
    }

    private AbstractSpan http(String name, String kind, String service) {
        AbstractSpan span = span(name, kind, service);
        span.getAttributes().put("http.request.method", "GET");
        return span;
    }

    private AbstractSpan kafka(String name, String kind, String service) {
        AbstractSpan span = span(name, kind, service);
        span.getAttributes().put("messaging.system", "kafka");
        // Exercise both attribute spellings present in the dataset.
        span.getAttributes().put(kind.equals("producer") ? "messaging.destination.name" : "messaging.destination", "checkout.orders");
        return span;
    }
}
