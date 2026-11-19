package at.scch.traceon.otel.graph;

import at.sfischer.constraints.data.InOutputDataSchema;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.data.model.*;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;
import at.sfischer.traces.otel.dataextraction.SpanData;
import at.sfischer.traces.otel.graph.GraphFragment;
import at.sfischer.traces.otel.graph.MatchingSpanFragmentBuilder;
import at.sfischer.traces.otel.matching.AttributeExistsMatch;
import at.sfischer.traces.otel.matching.AttributeMatch;
import at.sfischer.traces.otel.matching.KindMatch;
import at.sfischer.traces.otel.matching.TraceNodeMatch;

import java.util.List;

/** Fragment builders for the abstract spans emitted by Traceon's Main. */
public final class TraceonSpanFragmentBuilders {
    private TraceonSpanFragmentBuilders() {}

    private abstract static class SynchronousBuilder implements MatchingSpanFragmentBuilder {
        @Override
        public List<CallEdge> connectFragments(GraphFragment parent, ClusteredAbstractSpan parentSpan, GraphFragment child, ClusteredAbstractSpan childSpan, Graph graph) {
            return List.of(new SynchronousCallEdge(parent.exitNode(), child.entryNode(), schema(childSpan)));
        }
    }

    public static final class HttpServer extends SynchronousBuilder {
        @Override
        public TraceNodeMatch<ClusteredAbstractSpan> match() {
            return new KindMatch<ClusteredAbstractSpan>("server")
                    .and(new AttributeExistsMatch<>("http.request.method"));
        }

        @Override
        public List<CallEdge> connectFragments(GraphFragment parent, ClusteredAbstractSpan parentSpan, GraphFragment child, ClusteredAbstractSpan childSpan, Graph graph) {
            // The client already enters this endpoint. Continue into its service
            // without introducing an endpoint-to-itself call.
            if (parent.exitNode() == child.entryNode()){
                return child.internalEdges();
            }

            return super.connectFragments(parent, parentSpan, child, childSpan, graph);
        }

        @Override
        public GraphFragment buildFragment(ClusteredAbstractSpan span, Graph graph) {
            String service = attribute(span, "service.name", "unknown-service");
            String endpoint = httpOperation(span);
            GraphNode entry = node(graph, "http-server", service, endpoint);
            GraphNode exit = service(graph, service);
            return new GraphFragment(entry, exit, List.of(new SynchronousCallEdge(entry, exit, schema(span))));
        }
    }

    public static final class HttpClient extends SynchronousBuilder {
        @Override
        public TraceNodeMatch<ClusteredAbstractSpan> match() {
            return new KindMatch<ClusteredAbstractSpan>("client")
                    .and(new AttributeExistsMatch<>("http.request.method"));
        }

        @Override
        public GraphFragment buildFragment(ClusteredAbstractSpan span, Graph graph) {
            // Resolve the destination from the observed server, whose service name
            // and route are authoritative (the client address may be a DNS alias).
            List<ClusteredAbstractSpan> servers = span.getChildren().stream()
                    .filter(child -> new HttpServer().match().matches(child).isMatch())
                    .toList();
            if (servers.size() == 1) {
                ClusteredAbstractSpan server = servers.getFirst();
                return new GraphFragment(node(graph, "http-server",
                        attribute(server, "service.name", "unknown-service"), httpOperation(server)));
            }

            String destination = attribute(span, "server.address", "unknown-destination");
            return new GraphFragment(
                    node(graph, "http-client", destination, httpOperation(span))
            );
        }
    }

    public static final class Database extends SynchronousBuilder {
        private final String kind;

        public Database(String kind) {
            this.kind = kind;
        }

        @Override
        public TraceNodeMatch<ClusteredAbstractSpan> match() {
            return new KindMatch<>(kind);
        }

        @Override
        public GraphFragment buildFragment(ClusteredAbstractSpan span, Graph graph) {
            // No address is guaranteed in these captures. Scope by owning service
            // rather than merging every unnamed database into one shared node.
            return new GraphFragment(node(graph, "database", kind,
                    attribute(span, "db.name", "default")));
        }
    }

    public static final class Kafka implements MatchingSpanFragmentBuilder {
        @Override
        public TraceNodeMatch<ClusteredAbstractSpan> match() {
            return new AttributeMatch<>("messaging.system", "kafka");
        }

        @Override
        public GraphFragment buildFragment(ClusteredAbstractSpan span, Graph graph) {
            String topic = attribute(
                    span,
                    "messaging.destination.name",
                    attribute(span, "messaging.destination", "unknown-topic")
            );
            GraphNode topicNode = node(graph, "kafka-topic", topic);
            if ("producer".equalsIgnoreCase(span.getKind())) {
                GraphNode serviceNode = service(graph, attribute(span, "service.name", "unknown-service"));
                SimpleDataSchema inputSchema = span.getSpanDataCollection().getInputDataCollection().deriveSchema(new TraceonTypePromotionPolicy());
                return new GraphFragment(serviceNode, topicNode, List.of(new AsynchronousCallEdge(serviceNode, topicNode, inputSchema)));
            }

            GraphNode serviceNode = service(graph, attribute(span, "service.name", "unknown-service"));
            SimpleDataSchema inputSchema = span.getSpanDataCollection().getInputDataCollection().deriveSchema(new TraceonTypePromotionPolicy());
            return new GraphFragment(topicNode, serviceNode, List.of(new AsynchronousCallEdge(topicNode, serviceNode, inputSchema)));
        }

        @Override
        public List<CallEdge> connectFragments(
                GraphFragment parent,
                ClusteredAbstractSpan parentSpan,
                GraphFragment child,
                ClusteredAbstractSpan childSpan,
                Graph graph
        ) {
            // Producer and consumer fragments may share the same topic node.
            if (parent.exitNode() == child.entryNode()){
                return child.internalEdges();
            }

            SimpleDataSchema payloadSchema = childSpan.getSpanDataCollection()
                    .getInputDataCollection()
                    .deriveSchema(new TraceonTypePromotionPolicy());

            return List.of(new AsynchronousCallEdge(parent.exitNode(), child.entryNode(), payloadSchema));
        }
    }

    private static InOutputDataSchema<SimpleDataSchema> schema(ClusteredAbstractSpan span) {
        return span.getSpanDataCollection().deriveSchema(new TraceonTypePromotionPolicy());
    }

    private static String httpOperation(ClusteredAbstractSpan span) {
        String method = attribute(span, "http.request.method", "HTTP");
        String name = attribute(span, "http.route", span.getName());
        return name.equals(method) || name.startsWith(method + " ") ? name : method + " " + name;
    }

    private static String attribute(ClusteredAbstractSpan span, String key, String fallback) {
        Object value = span.getAttribute(key);
        return value == null ? fallback : value.toString();
    }

    private static GraphNode service(Graph graph, String name) {
        return node(graph, "service", name);
    }

    private static GraphNode node(Graph graph, String type, String... parts) {
        // Length prefixes keep IDs distinct even when names contain separators.
        StringBuilder id = new StringBuilder(type);
        for (String part : parts){
            id.append(':').append(part.length()).append(':').append(part);
        }

        return graph.getOrCreateNode(new GraphNode(id.toString(), String.join(" / ", parts)));
    }
}
