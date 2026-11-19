package at.scch.traceon.otel.graph;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;
import at.sfischer.traces.otel.dataextraction.SpanData;
import at.sfischer.traces.otel.graph.GraphFragment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TraceonSpanFragmentBuildersNodeTypeTest {

    @Test
    void observedHttpClientAndServerShareATypedEndpoint() {
        AbstractSpan server = http("server", "cart-service");
        AbstractSpan client = http("client", "checkout-service");
        client.addChild(server);
        ClusteredAbstractSpan clientSpan = new ClusteredAbstractSpan(client);
        Graph graph = new Graph();

        GraphFragment clientFragment = new TraceonSpanFragmentBuilders.HttpClient().buildFragment(clientSpan, graph);
        GraphFragment serverFragment = new TraceonSpanFragmentBuilders.HttpServer()
                .buildFragment(clientSpan.getChildren().getFirst(), graph);

        assertSame(clientFragment.entryNode(), serverFragment.entryNode());
        assertEquals("endpoint", clientFragment.entryNode().getType());
        assertEquals("service", serverFragment.exitNode().getType());
        assertEquals(2, graph.getNodes().size());
    }

    @Test
    void unobservedHttpClientKeepsItsIdAndHasEndpointType() {
        AbstractSpan client = http("client", "checkout-service");
        client.getAttributes().put("server.address", "cart-service");

        GraphFragment fragment = new TraceonSpanFragmentBuilders.HttpClient()
                .buildFragment(new ClusteredAbstractSpan(client), new Graph());

        assertEquals("endpoint", fragment.entryNode().getType());
        assertEquals("http-client:12:cart-service:10:GET /carts", fragment.entryNode().getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"MONGO", "REDIS"})
    void databaseBuildersAssignDatabaseType(String kind) {
        AbstractSpan database = span(kind, "cart-service");
        database.getAttributes().put("db.name", "cartdb");

        GraphFragment fragment = new TraceonSpanFragmentBuilders.Database(kind)
                .buildFragment(new ClusteredAbstractSpan(database), new Graph());

        assertEquals("database", fragment.entryNode().getType());
        assertEquals(kind + " / cartdb", fragment.entryNode().getName());
    }

    @Test
    void kafkaProducerAndConsumerShareATypedTopicAndReuseServiceNodes() {
        Graph graph = new Graph();
        AbstractSpan producer = kafka("producer", "checkout-service");
        AbstractSpan consumer = kafka("consumer", "order-service");
        TraceonSpanFragmentBuilders.Kafka builder = new TraceonSpanFragmentBuilders.Kafka();

        GraphFragment publication = builder.buildFragment(new ClusteredAbstractSpan(producer), graph);
        GraphFragment consumption = builder.buildFragment(new ClusteredAbstractSpan(consumer), graph);
        GraphFragment http = new TraceonSpanFragmentBuilders.HttpServer()
                .buildFragment(new ClusteredAbstractSpan(http("server", "order-service")), graph);

        assertSame(publication.exitNode(), consumption.entryNode());
        assertEquals("kafka-topic", publication.exitNode().getType());
        assertEquals("service", publication.entryNode().getType());
        assertEquals("service", consumption.exitNode().getType());
        assertSame(http.exitNode(), consumption.exitNode());
    }

    private static AbstractSpan http(String kind, String service) {
        AbstractSpan span = span(kind, service);
        span.getAttributes().put("http.request.method", "GET");
        span.getAttributes().put("http.route", "/carts");
        return span;
    }

    private static AbstractSpan kafka(String kind, String service) {
        AbstractSpan span = span(kind, service);
        span.getAttributes().put("messaging.system", "kafka");
        span.getAttributes().put("messaging.destination.name", "checkout.orders");
        return span;
    }

    private static AbstractSpan span(String kind, String service) {
        AbstractSpan span = new AbstractSpan("operation", kind);
        span.getAttributes().put("service.name", service);
        span.setSpanData(new SpanData(new DataObject(), new DataObject()));
        return span;
    }
}
