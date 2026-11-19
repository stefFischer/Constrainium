package at.scch.traceon.otel.graph;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.data.model.AsynchronousCallEdge;
import at.sfischer.constraints.data.model.CallEdge;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.constraints.data.model.DataFlow;
import at.sfischer.constraints.data.model.Edge;
import at.sfischer.constraints.data.model.GraphNode;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;
import at.sfischer.traces.otel.dataextraction.SpanData;
import at.sfischer.traces.otel.graph.GraphFragment;
import at.sfischer.traces.otel.graph.CallGraphBuilder;
import at.sfischer.traces.otel.graph.DispatchingSpanFragmentBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaSpanFragmentBuilderTest {

    @Test
    void infersMessageFieldsAcrossTheTopicIntoALeafConsumer() {
        ClusteredAbstractSpan parentSpan = createSpan(
                "consumer", "order-service", "checkout.orders", "{checkoutId:'checkout-1'}"
        );
        ClusteredAbstractSpan childSpan = createSpan(
                "consumer", "invoice-service", "order.created", "{orderId:'order-1'}"
        );
        parentSpan.addChild(childSpan);

        CallGraphBuilder builder = new CallGraphBuilder(
                new DispatchingSpanFragmentBuilder(new TraceonSpanFragmentBuilders.Kafka())
        );
        Graph graph = builder.build(parentSpan, new TraceonTypePromotionPolicy());
        AsynchronousCallEdge publication = findEdge(graph, "order-service", "order.created");
        AsynchronousCallEdge consumption = findEdge(graph, "order.created", "invoice-service");

        List<DataFlow> flows = publication.getDataFlowsTo(consumption);
        assertEquals(1, flows.size());

        DataSchemaEntry<SimpleDataSchema> publishedOrderId = publication.getSchema().findDataSchemaEntry("orderId");
        DataSchemaEntry<SimpleDataSchema> consumedOrderId = consumption.getSchema().findDataSchemaEntry("orderId");
        assertNotNull(publishedOrderId);
        assertNotNull(consumedOrderId);
        assertSame(consumedOrderId, flows.getFirst().getDataFlows().get(publishedOrderId));
    }

    @Test
    void doesNotInferAnInternalEdgeIntoItselfWhenTopicIsAlreadyShared() {
        ClusteredAbstractSpan producer = createSpan(
                "producer", "checkout-service", "checkout.orders", "{cartId:'cart-1'}"
        );
        ClusteredAbstractSpan consumer = createSpan(
                "consumer", "order-service", "checkout.orders", "{cartId:'cart-1'}"
        );
        producer.addChild(consumer);

        CallGraphBuilder builder = new CallGraphBuilder(
                new DispatchingSpanFragmentBuilder(new TraceonSpanFragmentBuilders.Kafka())
        );
        Graph graph = builder.build(producer, new TraceonTypePromotionPolicy());
        AsynchronousCallEdge consumption = findEdge(graph, "checkout.orders", "order-service");

        assertTrue(consumption.getDataFlowsTo(consumption).isEmpty());
    }

    private AsynchronousCallEdge findEdge(Graph graph, String from, String to) {
        for (GraphNode node : graph.getNodes()) {
            for (Edge edge : node.getOutgoing()) {
                if (edge instanceof AsynchronousCallEdge asynchronousEdge
                        && from.equals(edge.getFrom().getName())
                        && to.equals(edge.getTo().getName())) {
                    return asynchronousEdge;
                }
            }
        }

        throw new AssertionError("Missing edge " + from + " -> " + to);
    }

    @ParameterizedTest
    @ValueSource(strings = {"consumer", "producer"})
    void connectsFragmentsUsingTheChildMessageInput(String childKind) {
        TraceonSpanFragmentBuilders.Kafka builder = new TraceonSpanFragmentBuilders.Kafka();
        Graph graph = new Graph();

        ClusteredAbstractSpan parentSpan = createSpan("consumer", "order-service", "checkout.orders", "{checkoutId:'checkout-1'}");
        ClusteredAbstractSpan childSpan = createSpan(childKind, "invoice-service", "order.created", "{orderId:'order-1'}");
        GraphFragment parent = builder.buildFragment(parentSpan, graph);
        GraphFragment child = builder.buildFragment(childSpan, graph);

        List<CallEdge> connections = builder.connectFragments(parent, parentSpan, child, childSpan, graph);
        AsynchronousCallEdge connection = (AsynchronousCallEdge) connections.getFirst();

        assertSame(parent.exitNode(), connection.getFrom());
        assertSame(child.entryNode(), connection.getTo());
        assertNotNull(connection.getSchema().findDataSchemaEntry("orderId"));
        assertNull(connection.getSchema().findDataSchemaEntry("checkoutId"));
    }

    private ClusteredAbstractSpan createSpan(String kind, String service, String topic, String payload) {
        AbstractSpan span = new AbstractSpan(topic, kind);
        span.getAttributes().put("service.name", service);
        span.getAttributes().put("messaging.system", "kafka");
        span.getAttributes().put("messaging.destination.name", topic);
        span.setSpanData(new SpanData(DataObject.parseData(payload), null));

        return new ClusteredAbstractSpan(span);
    }
}
