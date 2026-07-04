package at.sfischer.traces.otel.graph;

import at.sfischer.constraints.data.*;
import at.sfischer.constraints.data.model.*;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;
import at.sfischer.traces.otel.dataextraction.SpanData;
import at.sfischer.traces.otel.matching.AttributeMatch;
import at.sfischer.traces.otel.matching.KindMatch;
import at.sfischer.traces.otel.matching.TraceNodeMatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class CallGraphBuilderTest {

    private static final String SERVER = "SERVER";
    private static final String CLIENT = "CLIENT";

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    private ClusteredAbstractSpan buildProductServiceTrace() {
        AbstractSpan root = new AbstractSpan("POST /products", SERVER);
        root.getAttributes().put("service.name", "product-service");
        root.getAttributes().put("http.request.method", "POST");
        root.getAttributes().put("http.route", "/products");
        root.getAttributes().put("http.response.status_code", 200L);
        root.getAttributes().put("server.address", "localhost");
        root.getAttributes().put("server.port", 8080L);
        root.getAttributes().put("url.scheme", "http");
        root.getAttributes().put("tracer", "io.opentelemetry.tomcat-10.0");
        root.setSpanData(new SpanData(
                DataObject.parseData("""
                        { "name": "Laptop", "price": 999.99 }
                        """),
                DataObject.parseData("""
                        { "id": "abc-123", "name": "Laptop", "price": 999.99 }
                        """)
        ));

        AbstractSpan select = new AbstractSpan("SELECT productdb.product", CLIENT);
        select.getAttributes().put("service.name", "product-service");
        select.getAttributes().put("db.operation", "SELECT");
        select.getAttributes().put("db.name", "productdb");
        select.getAttributes().put("db.sql.table", "product");
        select.getAttributes().put("tracer", "at.scch.jdbc");
        select.getAttributes().put("jdbc.sql",
                "select p1_0.id from product p1_0 where p1_0.id=? fetch first ? rows only");
        select.setSpanData(new SpanData(
                DataObject.parseData("""
                        { "id": "abc-123", "limit": 1 }
                        """),
                DataObject.parseData("""
                        {}
                        """)
        ));

        AbstractSpan insert = new AbstractSpan("INSERT productdb.product", CLIENT);
        insert.getAttributes().put("service.name", "product-service");
        insert.getAttributes().put("db.operation", "INSERT");
        insert.getAttributes().put("db.name", "productdb");
        insert.getAttributes().put("db.sql.table", "product");
        insert.getAttributes().put("tracer", "at.scch.jdbc");
        insert.getAttributes().put("jdbc.sql",
                "insert into product (name,price,id) values (?,?,?)");
        insert.setSpanData(new SpanData(
                DataObject.parseData("""
                        { "id": "abc-123", "name": "Laptop", "price": 999.99 }
                        """),
                DataObject.parseData("""
                        {}
                        """)
        ));

        root.addChild(select);
        root.addChild(insert);

        return new ClusteredAbstractSpan(root);
    }

    private CallGraphBuilder builder() {
        return new CallGraphBuilder(new SpanFragmentBuilder() {
            private final static TraceNodeMatch<ClusteredAbstractSpan> SERVER_MATCH =
                    new KindMatch<>("SERVER");

            private final static TraceNodeMatch<ClusteredAbstractSpan> JDBC_MATCH =
                    new KindMatch<ClusteredAbstractSpan>("CLIENT")
                            .and(new AttributeMatch<>("tracer", "at.scch.jdbc"));

            @Override
            public GraphFragment buildFragment(ClusteredAbstractSpan span, Graph graph) {
                if(matches(span, SERVER_MATCH)){
                    String endpointName = span.getName();
                    String serviceName = span.getAttribute("service.name");

                    GraphNode endpointNode = new GraphNode(endpointName, endpointName);
                    GraphNode serviceNode = new GraphNode(serviceName, serviceName);
                    endpointNode = graph.getOrCreateNode(endpointNode);
                    serviceNode = graph.getOrCreateNode(serviceNode);

                    InOutputDataCollection data = span.getSpanDataCollection();
                    InOutputDataSchema<SimpleDataSchema> schema = data.deriveSchema(new DefaultTypePromotionPolicy());
                    CallEdge internalEdge = new SynchronousCallEdge(endpointNode, serviceNode, schema);

                    return new GraphFragment(endpointNode, serviceNode, List.of(internalEdge));
                }

                if(matches(span, JDBC_MATCH)){
                    String dbName = span.getAttribute("db.name");
                    GraphNode node = new GraphNode(dbName, dbName);
                    node = graph.getOrCreateNode(node);
                    return new GraphFragment(node);
                }

                return null;
            }

            @Override
            public List<CallEdge> connectFragments(GraphFragment parent, ClusteredAbstractSpan parentSpan, GraphFragment child, ClusteredAbstractSpan childSpan, Graph graph) {
                if(matches(childSpan, JDBC_MATCH)){
                    InOutputDataCollection data = childSpan.getSpanDataCollection();
                    InOutputDataSchema<SimpleDataSchema> schema = data.deriveSchema(new DefaultTypePromotionPolicy());
                    String query = childSpan.getAttribute("jdbc.sql");
                    DatabaseQueryEdge dbEdge = DatabaseQueryEdge.findQueryEdge(parent.exitNode(), query);
                    if (dbEdge != null) {
                        dbEdge.getSchema().unify(schema, new DefaultTypePromotionPolicy());
                    }
                    return List.of(Objects.requireNonNullElseGet(dbEdge, () -> new DatabaseQueryEdge(parent.exitNode(), child.entryNode(), schema, query)));
                }

                return List.of();
            }

            private static boolean matches(ClusteredAbstractSpan span , TraceNodeMatch<ClusteredAbstractSpan> match){
                return match.matches(span).isMatch();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Structural graph tests
    // -------------------------------------------------------------------------

    @Test
    void graphContainsExpectedNodes() {
        Graph graph = builder().build(buildProductServiceTrace());

        Set<String> nodeNames = graph.getNodes().stream()
                .map(GraphNode::getName)
                .collect(Collectors.toSet());

        assertThat(nodeNames).containsExactlyInAnyOrder(
                "POST /products", "product-service", "productdb"
        );
    }

    @Test
    void endpointNodeIsRegisteredAsEntryNode() {
        Graph graph = builder().build(buildProductServiceTrace());

        Set<String> entryNames = graph.getEntryNodes().stream()
                .map(GraphNode::getName)
                .collect(Collectors.toSet());

        assertThat(entryNames).containsExactly("POST /products");
    }

    @Test
    void endpointToServiceEdgeIsSynchronous() {
        Graph graph = builder().build(buildProductServiceTrace());

        GraphNode endpoint = graph.getNodes().stream()
                .filter(n -> n.getName().equals("POST /products"))
                .findFirst().orElseThrow();

        assertThat(endpoint.getOutgoing())
                .hasSize(1)
                .allMatch(e -> e instanceof SynchronousCallEdge);
    }

    @Test
    void serviceNodeHasTwoDatabaseQueryEdges() {
        Graph graph = builder().build(buildProductServiceTrace());

        GraphNode serviceNode = graph.getNodes().stream()
                .filter(n -> n.getName().equals("product-service"))
                .findFirst().orElseThrow();

        List<Edge> dbEdges = serviceNode.getOutgoing().stream()
                .filter(e -> e instanceof DatabaseQueryEdge)
                .toList();

        assertThat(dbEdges).hasSize(2);
    }

    @Test
    void selectAndInsertEdgesHaveCorrectQueryStrings() {
        Graph graph = builder().build(buildProductServiceTrace());

        GraphNode serviceNode = graph.getNodes().stream()
                .filter(n -> n.getName().equals("product-service"))
                .findFirst().orElseThrow();

        Set<String> queries = serviceNode.getOutgoing().stream()
                .filter(e -> e instanceof DatabaseQueryEdge)
                .map(e -> ((DatabaseQueryEdge) e).getQuery())
                .collect(Collectors.toSet());

        assertThat(queries).containsExactlyInAnyOrder(
                "select p1_0.id from product p1_0 where p1_0.id=? fetch first ? rows only",
                "insert into product (name,price,id) values (?,?,?)"
        );
    }

    @Test
    void databaseQueryEdgesBothPointToProductdb() {
        Graph graph = builder().build(buildProductServiceTrace());

        GraphNode serviceNode = graph.getNodes().stream()
                .filter(n -> n.getName().equals("product-service"))
                .findFirst().orElseThrow();

        assertThat(serviceNode.getOutgoing().stream()
                .filter(e -> e instanceof DatabaseQueryEdge)
                .map(e -> e.getTo().getName())
                .collect(Collectors.toSet()))
                .containsExactly("productdb");
    }

    // -------------------------------------------------------------------------
    // Data flow tests
    // -------------------------------------------------------------------------

    private DatabaseQueryEdge findEdgeByQuery(Graph graph, String query) {
        return graph.getNodes().stream()
                .flatMap(n -> n.getOutgoing().stream())
                .filter(e -> e instanceof DatabaseQueryEdge)
                .map(e -> (DatabaseQueryEdge) e)
                .filter(e -> e.getQuery().equals(query))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No DatabaseQueryEdge found for query: " + query));
    }

    @Test
    void insertEdgeHasDataFlowsInferred() {
        Graph graph = builder().build(buildProductServiceTrace());

        DatabaseQueryEdge insertEdge = findEdgeByQuery(graph,
                "insert into product (name,price,id) values (?,?,?)");

        assertThat(insertEdge.getDataFlows()).isNotEmpty();
    }

    @Test
    void selectEdgeHasDataFlowsInferred() {
        Graph graph = builder().build(buildProductServiceTrace());

        DatabaseQueryEdge selectEdge = findEdgeByQuery(graph,
                "select p1_0.id from product p1_0 where p1_0.id=? fetch first ? rows only");

        assertThat(selectEdge.getDataFlows()).isNotEmpty();
    }

    @Test
    void insertEdgeDataFlowMapsIdFromRootOutput() {
        Graph graph = builder().build(buildProductServiceTrace());

        DatabaseQueryEdge insertEdge = findEdgeByQuery(graph,
                "insert into product (name,price,id) values (?,?,?)");

        // DataFlow maps source DataSchemaEntry → target DataSchemaEntry
        // The INSERT input schema should receive "id" mapped from the root output
        boolean hasIdMapping = insertEdge.getDataFlows().stream()
                .flatMap(df -> df.getDataFlows().entrySet().stream())
                .anyMatch(entry -> entry.getValue().name.equals("id"));

        assertThat(hasIdMapping).isTrue();
    }

    @Test
    void allDataFlowsShareSingleTraceId() {
        Graph graph = builder().build(buildProductServiceTrace());

        List<String> traceIds = graph.getNodes().stream()
                .flatMap(n -> n.getOutgoing().stream())
                .filter(e -> e instanceof CallEdge)
                .map(e -> (CallEdge) e)
                .flatMap(e -> e.getDataFlows().stream())
                .map(DataFlow::getTraceId)
                .distinct()
                .toList();

        assertThat(traceIds).hasSize(1);
    }

    @Test
    void twoBuildsProduceDifferentTraceIds() {
        CallGraphBuilder builder = builder();

        Graph graph1 = builder.build(buildProductServiceTrace());
        Graph graph2 = builder.build(buildProductServiceTrace());

        Set<String> ids1 = collectTraceIds(graph1);
        Set<String> ids2 = collectTraceIds(graph2);

        assertThat(ids1).isNotEmpty();
        assertThat(ids2).isNotEmpty();
        assertThat(ids1).doesNotContainAnyElementsOf(ids2);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Set<String> collectTraceIds(Graph graph) {
        return graph.getNodes().stream()
                .flatMap(n -> n.getOutgoing().stream())
                .filter(e -> e instanceof CallEdge)
                .map(e -> (CallEdge) e)
                .flatMap(e -> e.getDataFlows().stream())
                .map(DataFlow::getTraceId)
                .collect(Collectors.toSet());
    }
}
