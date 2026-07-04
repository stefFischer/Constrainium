package at.sfischer.traces.otel.graph;

import at.sfischer.constraints.data.*;
import at.sfischer.constraints.data.model.*;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.abstraction.SpanAbstractor;
import at.sfischer.traces.otel.abstraction.TraceAbstractor;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpanAccumulator;
import at.sfischer.traces.otel.clustering.TraceCluster;
import at.sfischer.traces.otel.clustering.TraceClusterer;
import at.sfischer.traces.otel.collector.FileCollector;
import at.sfischer.traces.otel.collector.buffer.TraceBuffer;
import at.sfischer.traces.otel.dataextraction.SpanData;
import at.sfischer.traces.otel.dataextraction.StorageSpanData;
import at.sfischer.traces.otel.dataextraction.rest.RestSpanDataExtractor;
import at.sfischer.traces.otel.dataextraction.sql.SQLDataExtractor;
import at.sfischer.traces.otel.differ.AndSpanComparator;
import at.sfischer.traces.otel.differ.AttributesSpanComparator;
import at.sfischer.traces.otel.differ.NameAndKindSpanComparator;
import at.sfischer.traces.otel.differ.SpanComparator;
import at.sfischer.traces.otel.filter.MatchFilter;
import at.sfischer.traces.otel.filter.MinSpanCountFilter;
import at.sfischer.traces.otel.matching.*;
import at.sfischer.traces.otel.parser.OtelResourceSpansParser;
import at.sfischer.traces.otel.processing.TraceProcessor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;

import static at.sfischer.traces.otel.dataextraction.SpanAttributeExtractor.attribute;
import static org.assertj.core.api.Assertions.assertThat;

public class CallGraphBuilderIntegrationTest {

    private static final TraceAbstractor ABSTRACTOR = new TraceAbstractor (
            new SpanAbstractor(SpanMatch.kind("SERVER")){
                @Override
                public AbstractSpan abstractSpan(Span span) {
                    AbstractSpan abstractSpan = new AbstractSpan(span.getName(), span.getKind());
                    abstractSpan.getAttributes().put("tracer", span.getTracer());
                    transferAttributes(span, abstractSpan,
                            "service.name",
                            "server.address",
                            "server.port",
                            "url.scheme",
                            "http.response.status_code",
                            "http.request.method",
                            "http.route",
                            "url.path",
                            "http.request.body",
                            "http.response.body");

                    RestSpanDataExtractor dataExtractor = new RestSpanDataExtractor(
                            attribute("http.route"),
                            attribute("url.path"),
                            attribute("http.request.body"),
                            attribute("http.response.body")
                    );
                    SpanData data = dataExtractor.extractData(abstractSpan);
                    abstractSpan.setSpanData(data);
                    abstractSpan.removeAttribute("url.path");
                    abstractSpan.removeAttribute("http.request.body");
                    abstractSpan.removeAttribute("http.response.body");

                    return abstractSpan;
                }
            },
            new SpanAbstractor(SpanMatch.kind("CLIENT")
                    .and(SpanMatch.tracer("at.scch.jdbc"))
                    .and(SpanMatch.name("EXECUTE SQL"))) {
                @Override
                public AbstractSpan abstractSpan(Span span) {
                    Span child = span.findChild(SpanMatch.tracer("io.opentelemetry.jdbc")
                            .and(SpanMatch.attributeExists("db.sql.table")));
                    if (child == null) {
                        return null;
                    }

                    AbstractSpan abstractSpan = new AbstractSpan(child.getName(), span.getKind());
                    abstractSpan.getAttributes().put("tracer", span.getTracer());
                    transferAttributes(span, abstractSpan,
                            "service.name",
                            "jdbc.sql",
                            "jdbc.arguments",
                            "jdbc.parameterTypes");

                    transferAttributes(child, abstractSpan,
                            "db.name",
                            "db.sql.table",
                            "db.operation");

                    Long statementId = span.getAttribute("jdbc.statement");
                    Span resultSpan = span.findSibling(SpanMatch.tracer("at.scch.jdbc")
                            .and(SpanMatch.name("RESULTS"))
                            .and(SpanMatch.attribute("jdbc.statement", statementId)));

                    if (resultSpan != null) {
                        transferAttributes(resultSpan, abstractSpan, "jdbc.results");
                    }

                    SQLDataExtractor dataExtractor = new SQLDataExtractor(
                            attribute("jdbc.sql"),
                            attribute("jdbc.arguments"),
                            attribute("jdbc.results")
                    );
                    StorageSpanData data = dataExtractor.extractData(abstractSpan);
                    abstractSpan.setSpanData(data);
                    abstractSpan.removeAttribute("jdbc.arguments");
                    abstractSpan.removeAttribute("jdbc.results");
                    abstractSpan.removeAttribute("jdbc.results");

                    return abstractSpan;
                }
            },
            new SpanAbstractor(SpanMatch.tracerRegex(".*kafka.*")){
                @Override
                public AbstractSpan abstractSpan(Span span) {
                    AbstractSpan abstractSpan = new AbstractSpan(span.getName(), span.getKind());
                    abstractSpan.getAttributes().put("tracer", span.getTracer());
                    transferAttributes(span, abstractSpan,"service.name");
                    // TODO Instrument and collect Kafka data.
                    abstractSpan.setSpanData(new SpanData(new DataObject(), new DataObject()));

                    return abstractSpan;
                }
            }
    );

    private static TraceClusterer<AbstractSpan, ClusteredAbstractSpan> getAbstractSpanInOutputDataCollectionTraceClusterer() {
        SpanComparator<AbstractSpan> clusterComparator = new AndSpanComparator<>(
                new NameAndKindSpanComparator<AbstractSpan>(),
                new AttributesSpanComparator<>()
        );
        return new TraceClusterer<>(clusterComparator, new ClusteredAbstractSpanAccumulator());
    }

    private static class ServerSpanFragmentBuilder implements MatchingSpanFragmentBuilder {

        private static final TraceNodeMatch<ClusteredAbstractSpan> MATCH =
                new KindMatch<>("SERVER");

        @Override
        public TraceNodeMatch<ClusteredAbstractSpan> match() {
            return MATCH;
        }

        @Override
        public GraphFragment buildFragment(ClusteredAbstractSpan span, Graph graph) {
            String endpointName = span.getName();
            String serviceName  = span.getAttribute("service.name");

            GraphNode endpointNode = graph.getOrCreateNode(new GraphNode(endpointName, endpointName));
            GraphNode serviceNode  = graph.getOrCreateNode(new GraphNode(serviceName,  serviceName));

            InOutputDataCollection data = span.getSpanDataCollection();
            InOutputDataSchema<SimpleDataSchema> schema = data.deriveSchema(new DefaultTypePromotionPolicy());
            CallEdge internalEdge = new SynchronousCallEdge(endpointNode, serviceNode, schema);

            return new GraphFragment(endpointNode, serviceNode, List.of(internalEdge));
        }

        @Override
        public List<CallEdge> connectFragments(
                GraphFragment parent, ClusteredAbstractSpan parentSpan,
                GraphFragment child, ClusteredAbstractSpan childSpan,
                Graph graph
        ) {
            InOutputDataCollection data = childSpan.getSpanDataCollection();
            InOutputDataSchema<SimpleDataSchema> schema = data.deriveSchema(new DefaultTypePromotionPolicy());
            return List.of(new SynchronousCallEdge(parent.exitNode(), child.entryNode(), schema));
        }
    }

    private static class JdbcSpanFragmentBuilder implements MatchingSpanFragmentBuilder {

        private static final TraceNodeMatch<ClusteredAbstractSpan> MATCH =
                new KindMatch<ClusteredAbstractSpan>("CLIENT")
                        .and(new AttributeMatch<>("tracer", "at.scch.jdbc"));

        @Override
        public TraceNodeMatch<ClusteredAbstractSpan> match() {
            return MATCH;
        }

        @Override
        public GraphFragment buildFragment(ClusteredAbstractSpan span, Graph graph) {
            String dbName = span.getAttribute("db.name");
            GraphNode node = graph.getOrCreateNode(new GraphNode(dbName, dbName));
            return new GraphFragment(node);
        }

        @Override
        public List<CallEdge> connectFragments(
                GraphFragment parent, ClusteredAbstractSpan parentSpan,
                GraphFragment child, ClusteredAbstractSpan childSpan,
                Graph graph
        ) {
            InOutputDataCollection data = childSpan.getSpanDataCollection();
            InOutputDataSchema<SimpleDataSchema> schema = data.deriveSchema(new DefaultTypePromotionPolicy());
            String query = childSpan.getAttribute("jdbc.sql");

            DatabaseQueryEdge existing = DatabaseQueryEdge.findQueryEdge(parent.exitNode(), query);
            if (existing != null) {
                existing.getSchema().unify(schema, new DefaultTypePromotionPolicy());
                return List.of(existing);
            }

            return List.of(new DatabaseQueryEdge(parent.exitNode(), child.entryNode(), schema, query));
        }
    }

    private static class KafkaSpanFragmentBuilder implements MatchingSpanFragmentBuilder {

        private static final TraceNodeMatch<ClusteredAbstractSpan> MATCH =
                new AttributeRegexMatch<>("tracer", ".*kafka.*");

        @Override
        public TraceNodeMatch<ClusteredAbstractSpan> match() {
            return MATCH;
        }

        @Override
        public GraphFragment buildFragment(ClusteredAbstractSpan span, Graph graph) {
            GraphNode node = graph.getOrCreateNode(new GraphNode(span.getName(), span.getName()));
            return new GraphFragment(node);
        }

        @Override
        public List<CallEdge> connectFragments(
                GraphFragment parent, ClusteredAbstractSpan parentSpan,
                GraphFragment child, ClusteredAbstractSpan childSpan,
                Graph graph
        ) {
            SimpleDataCollection data = childSpan.getSpanDataCollection().getOutputDataCollection();
            SimpleDataSchema schema = data.deriveSchema(new DefaultTypePromotionPolicy());
            return List.of(new AsynchronousCallEdge(parent.exitNode(), child.entryNode(), schema));
        }
    }

    CallGraphBuilder CALL_GRAPH_BUILDER = new CallGraphBuilder(
            new DispatchingSpanFragmentBuilder(
                    new ServerSpanFragmentBuilder(),
                    new JdbcSpanFragmentBuilder(),
                    new KafkaSpanFragmentBuilder()
            )
    );

    @Test
    public void parsePipelineTest() throws URISyntaxException {
        File file = Paths.get(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResource("integrationTest/traces.json")
                ).toURI()
        ).toFile();

        OtelResourceSpansParser parser = new OtelResourceSpansParser();
        FileCollector collector = new FileCollector(file, parser);
        TraceBuffer buffer = new TraceBuffer(5);
        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = getAbstractSpanInOutputDataCollectionTraceClusterer();

        TraceProcessor<Span, AbstractSpan> pipeline = ABSTRACTOR
                .pipe(new MinSpanCountFilter<>(2))
                .pipe(new MatchFilter<>(AbstractSpanMatch.kind("SERVER")))
                .pipe(input -> {
                    assertThat(input).allSatisfy(span -> {
                        assertThat(span.getKind()).isEqualTo("SERVER"); // MatchFilter worked
                        assertThat(span.countSpans()).isGreaterThanOrEqualTo(2); // MinSpanCountFilter worked
                    });

                    for (AbstractSpan span : input) {
                        clusterer.add(span);
                    }

                    return input;
                });

        buffer.addTraceListener(pipeline::process);

        collector.addTraceListener(buffer);
        collector.collect();

        buffer.flush();

        assertThat(clusterer.getClusters()).isNotEmpty();

        clusterer.getClusters().forEach(cluster -> {
            ClusteredAbstractSpan state = cluster.getState();

            // SpanData count must match across all children at every level —
            // if addSpan threw, the cluster would not have been built
            int rootDataCount = state.getSpanData().size();
            assertThat(rootDataCount).isGreaterThan(0);

            assertThat(state.getChildren()).allSatisfy(child ->
                    assertThat(child.getSpanData().size()).isEqualTo(rootDataCount)
            );
        });

        List<ClusteredAbstractSpan> clusteredAbstractSpans = new LinkedList<>();
        for (TraceCluster<AbstractSpan, ClusteredAbstractSpan> cluster : clusterer.getClusters()) {
            ClusteredAbstractSpan clusteredSpan = cluster.getState();
            clusteredAbstractSpans.add(clusteredSpan);
        }

        for (ClusteredAbstractSpan clusteredSpan : clusteredAbstractSpans) {
            Graph g = CALL_GRAPH_BUILDER.build(clusteredSpan);

            assertThat(g.getNodes()).isNotEmpty();
            assertThat(g.getEntryNodes()).isNotEmpty();

            // Every entry node should be an endpoint node (from a SERVER span)
            g.getEntryNodes().forEach(entry ->
                    assertThat(entry.getOutgoing())
                            .anyMatch(e -> e instanceof SynchronousCallEdge)
            );

            // Every node should be reachable from an entry node
            Set<GraphNode> reachable = collectReachable(g.getEntryNodes());
            assertThat(reachable).containsAll(g.getNodes());

            // No edge should have a null from or to
            g.getNodes().stream()
                    .flatMap(n -> n.getOutgoing().stream())
                    .forEach(e -> {
                        assertThat(e.getFrom()).isNotNull();
                        assertThat(e.getTo()).isNotNull();
                    });

            // Every DatabaseQueryEdge should have a non-blank query string
            g.getNodes().stream()
                    .flatMap(n -> n.getOutgoing().stream())
                    .filter(e -> e instanceof DatabaseQueryEdge)
                    .map(e -> (DatabaseQueryEdge) e)
                    .forEach(e -> assertThat(e.getQuery()).isNotBlank());
        }
    }

    private Set<GraphNode> collectReachable(Set<GraphNode> entryNodes) {
        Set<GraphNode> visited = new HashSet<>();
        Deque<GraphNode> queue = new ArrayDeque<>(entryNodes);
        while (!queue.isEmpty()) {
            GraphNode node = queue.poll();
            if (visited.add(node)) {
                node.getOutgoing().stream()
                        .map(Edge::getTo)
                        .forEach(queue::add);
            }
        }
        return visited;
    }
}
