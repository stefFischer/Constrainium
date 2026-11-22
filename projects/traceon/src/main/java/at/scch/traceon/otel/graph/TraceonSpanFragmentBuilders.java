package at.scch.traceon.otel.graph;

import at.sfischer.constraints.data.model.*;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;
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
            if(parent.exitNode().equals(child.entryNode())){
                return List.of();
            }

            return List.of(new SynchronousCallEdge(parent.exitNode(), child.entryNode()));
        }
    }

    private abstract static class HttpBuilder extends SynchronousBuilder {
        @Override
        public List<CallEdge> connectFragments(GraphFragment parent, ClusteredAbstractSpan parentSpan,
                                              GraphFragment child, ClusteredAbstractSpan childSpan, Graph graph) {
            if (parent.exitNode() == child.entryNode()) {
                return List.of();
            }

            return List.of(httpEdge(parent.exitNode(), child.entryNode()));
        }
    }

    public static final class HttpServer extends HttpBuilder {
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
            GraphNode entry = node(graph, "http-server", "endpoint", service, endpoint);
            GraphNode exit = service(graph, service);
            return new GraphFragment(entry, exit, List.of(httpEdge(entry, exit)));
        }
    }

    public static final class HttpClient extends HttpBuilder {
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
                return new GraphFragment(node(graph, "http-server", "endpoint",
                        attribute(server, "service.name", "unknown-service"), httpOperation(server)));
            }

            String destination = attribute(span, "server.address", "unknown-destination");
            GraphNode endpoint = node(graph, "http-client", "endpoint", destination, httpOperation(span));

            String destinationService = servers.isEmpty() ? inferDestinationService(span) : null;
            if (destinationService != null) {
                GraphNode service = service(graph, destinationService);
                CallEdge internalEdge = httpEdge(endpoint, service);
                return new GraphFragment(endpoint, service, List.of(internalEdge));
            }

            return new GraphFragment(endpoint);
        }

        private String inferDestinationService(ClusteredAbstractSpan span) {
            String caller = attribute(span, "service.name", null);
            // Children from another service reveal the receiver when its server span is missing.
            List<String> services = span.getChildren().stream()
                    .map(child -> attribute(child, "service.name", null))
                    .filter(service -> service != null && !service.isBlank() && !service.equals(caller))
                    .distinct()
                    .toList();
            return services.size() == 1 ? services.getFirst() : null;
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
        public List<CallEdge> connectFragments(GraphFragment parent, ClusteredAbstractSpan parentSpan, GraphFragment child, ClusteredAbstractSpan childSpan, Graph graph) {
            if (parent.exitNode() == child.entryNode()) {
                return List.of();
            }

            String operation = attribute(childSpan, "db.operation.name", childSpan.getName());
            String query = attribute(childSpan, "db.query.text", attribute(childSpan, "db.statement", operation));
            DatabaseQueryEdge existing = DatabaseQueryEdge.findQueryEdge(parent.exitNode(), child.entryNode(), query);
            if (existing != null) {
                return List.of(existing);
            }

            return List.of(new DatabaseQueryEdge(parent.exitNode(), child.entryNode(), query));
        }

        @Override
        public GraphFragment buildFragment(ClusteredAbstractSpan span, Graph graph) {
            // No address is guaranteed in these captures. Scope by owning service
            // rather than merging every unnamed database into one shared node.
            return new GraphFragment(node(graph, "database", "database", kind,
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
            GraphNode topicNode = node(graph, "kafka-topic", "kafka-topic", topic);
            if ("producer".equalsIgnoreCase(span.getKind())) {
                GraphNode serviceNode = service(graph, attribute(span, "service.name", "unknown-service"));
                return new GraphFragment(serviceNode, topicNode, List.of(new AsynchronousCallEdge(serviceNode, topicNode)));
            }

            GraphNode serviceNode = service(graph, attribute(span, "service.name", "unknown-service"));
            return new GraphFragment(topicNode, serviceNode, List.of(new AsynchronousCallEdge(topicNode, serviceNode)));
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

            return List.of(new AsynchronousCallEdge(parent.exitNode(), child.entryNode()));
        }
    }

    private static SynchronousCallEdge httpEdge(GraphNode from, GraphNode to) {
        for (Edge edge : from.getOutgoing()) {
            if (edge.getClass() == SynchronousCallEdge.class && edge.getTo() == to) {
                return (SynchronousCallEdge) edge;
            }
        }

        return new SynchronousCallEdge(from, to);
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
        return node(graph, "service", "service", name);
    }

    private static GraphNode node(Graph graph, String idCategory, String type, String... parts) {
        // Length prefixes keep IDs distinct even when names contain separators.
        StringBuilder id = new StringBuilder(idCategory);
        for (String part : parts){
            id.append(':').append(part.length()).append(':').append(part);
        }

        return graph.getOrCreateNode(new GraphNode(id.toString(), String.join(" / ", parts), type));
    }
}
