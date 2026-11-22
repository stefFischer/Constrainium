package at.scch.traceon.otel.graph;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.data.model.CallEdge;
import at.sfischer.constraints.data.model.DataFlow;
import at.sfischer.constraints.data.model.DataPathNode;
import at.sfischer.constraints.data.model.DatabaseQueryEdge;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.constraints.data.model.GraphNode;
import at.sfischer.constraints.data.model.utils.GraphJsonExporter;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;
import at.sfischer.traces.otel.dataextraction.SpanData;
import at.sfischer.traces.otel.graph.CallGraphBuilder;
import at.sfischer.traces.otel.graph.DispatchingSpanFragmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductCategoryDataFlowTest {

    @Test
    void productAndCategoryLookupsKeepTheirOwnComparableObservationsOnOneFindEdge() {
        Graph graph = build(checkout("a", "P-a", "product-a"), checkout("b", "P-b", "product-b"));
        DatabaseQueryEdge query = query(graph);
        assertEquals(2, query.getDataByTrace().get("trace-a").size());
        assertEquals(2, query.getDataByTrace().get("trace-b").size());

        graph.finalizeGraph();

        CallEdge product = request(graph, "/products/{productId}");
        CallEdge category = request(graph, "/products");
        assertMappings(product, query, "input.path_params.productId", "input.payload.filter.sku");
        assertMappings(category, query, "input.query_params.category", "input.payload.filter.category");
        assertMappings(query, product, "output.payload.result", "output.payload.value");
        assertMappings(query, category, "output.payload.result", "output.payload.value");
        assertPaths(product, "input.path_params.productId", query, 0);
        assertPaths(category, "input.query_params.category", query, 1);
        assertTrue(query.getDataByTrace().isEmpty());
        assertTrue(graph.getCallRelations().isEmpty());
        assertDoesNotThrow(() -> GraphJsonExporter.export(graph));
    }

    @Test
    void aProductRequestCounterexampleInAnotherExecutionRejectsTheWholeProductMapping() {
        Graph graph = build(checkout("a", "P-a", "product-a"), checkout("b", "WRONG", "product-b"));

        graph.finalizeGraph();

        DatabaseQueryEdge query = query(graph);
        CallEdge product = request(graph, "/products/{productId}");
        CallEdge category = request(graph, "/products");
        assertNoMapping(product, query, "input.path_params.productId");
        assertMappings(category, query, "input.query_params.category", "input.payload.filter.category");
        assertMappings(query, product, "output.payload.result", "output.payload.value");
        assertMappings(query, category, "output.payload.result", "output.payload.value");
    }

    @Test
    void aProductResponseCounterexampleInAnotherExecutionRejectsTheWholeResponseMapping() {
        Graph graph = build(checkout("a", "P-a", "product-a"), checkout("b", "P-b", "WRONG"));

        graph.finalizeGraph();

        DatabaseQueryEdge query = query(graph);
        CallEdge product = request(graph, "/products/{productId}");
        CallEdge category = request(graph, "/products");
        DataSchemaEntry<SimpleDataSchema> result = query.getSchema().findDataSchemaEntry("output.payload.result");
        assertEquals(2, query.getDataFlowsTo(product).size());
        assertTrue(query.getDataFlowsTo(product).stream().allMatch(flow -> !flow.getDataFlows().containsKey(result)));
        assertMappings(product, query, "input.path_params.productId", "input.payload.filter.sku");
        assertMappings(category, query, "input.query_params.category", "input.payload.filter.category");
        assertMappings(query, category, "output.payload.result", "output.payload.value");
    }

    private static Graph build(AbstractSpan first, AbstractSpan second) {
        ClusteredAbstractSpan cluster = new ClusteredAbstractSpan(first);
        cluster.addSpan(second);
        CallGraphBuilder builder = new CallGraphBuilder(new DispatchingSpanFragmentBuilder(
                new TraceonSpanFragmentBuilders.HttpServer(), new TraceonSpanFragmentBuilders.HttpClient(),
                new TraceonSpanFragmentBuilders.Database("MONGO")));
        return builder.buildMutable(cluster, List.of("trace-a", "trace-b"));
    }

    private static AbstractSpan checkout(String suffix, String productLookupId, String productResult) {
        AbstractSpan root = http("server", "checkout-service", "POST", "/checkout", "{}", "{}");
        root.addChild(lookup("/products/{productId}",
                "{path_params:{productId:'P-%s'}}".formatted(suffix),
                "{payload:{value:'product-%s'}}".formatted(suffix),
                "{payload:{find:'products',filter:{sku:'%s'}}}".formatted(productLookupId),
                "{payload:{result:'%s'}}".formatted(productResult)));
        root.addChild(lookup("/products",
                "{query_params:{category:'C-%s'}}".formatted(suffix),
                "{payload:{value:'category-%s'}}".formatted(suffix),
                "{payload:{find:'products',filter:{category:'C-%s'}}}".formatted(suffix),
                "{payload:{result:'category-%s'}}".formatted(suffix)));
        return root;
    }

    private static AbstractSpan lookup(String route, String input, String output, String queryInput, String queryOutput) {
        AbstractSpan client = http("client", "checkout-service", "GET", route, input, output);
        AbstractSpan server = http("server", "product-service", "GET", route, input, output);
        AbstractSpan query = new AbstractSpan("find", "MONGO");
        query.getAttributes().put("db.name", "product_svc");
        query.getAttributes().put("db.operation.name", "find");
        query.setSpanData(new SpanData(DataObject.parseData(queryInput), DataObject.parseData(queryOutput)));
        client.addChild(server);
        server.addChild(query);
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

    private static GraphNode productService(Graph graph) {
        return graph.getNodes().stream().filter(node -> node.getName().equals("product-service"))
                .findFirst().orElseThrow();
    }

    private static DatabaseQueryEdge query(Graph graph) {
        List<DatabaseQueryEdge> queries = productService(graph).getOutgoing().stream()
                .map(edge -> assertInstanceOf(DatabaseQueryEdge.class, edge)).toList();
        assertEquals(1, queries.size());
        assertEquals("find", queries.getFirst().getQuery());
        return queries.getFirst();
    }

    private static CallEdge request(Graph graph, String route) {
        return productService(graph).getIncoming().stream()
                .filter(edge -> edge.getFrom().getName().endsWith("GET " + route))
                .map(CallEdge.class::cast).findFirst().orElseThrow();
    }

    private static void assertMappings(CallEdge source, CallEdge target, String sourcePath, String targetPath) {
        DataSchemaEntry<SimpleDataSchema> from = source.getSchema().findDataSchemaEntry(sourcePath);
        DataSchemaEntry<SimpleDataSchema> to = target.getSchema().findDataSchemaEntry(targetPath);
        assertNotNull(from);
        assertNotNull(to);
        List<DataFlow> flows = source.getDataFlowsTo(target);
        assertEquals(2, flows.size());
        for (DataFlow flow : flows) {
            assertSame(to, flow.getDataFlows().get(from));
        }
    }

    private static void assertNoMapping(CallEdge source, CallEdge target, String sourcePath) {
        DataSchemaEntry<SimpleDataSchema> from = source.getSchema().findDataSchemaEntry(sourcePath);
        assertEquals(2, source.getDataFlowsTo(target).size());
        for (DataFlow flow : source.getDataFlowsTo(target)) {
            assertFalse(flow.getDataFlows().containsKey(from));
        }
        assertTrue(source.getFrom().deriveDataPaths(from).getRoots().isEmpty());
    }

    private static void assertPaths(CallEdge source, String fieldPath, CallEdge target, int targetRow) {
        DataSchemaEntry<SimpleDataSchema> field = source.getSchema().findDataSchemaEntry(fieldPath);
        List<DataPathNode> roots = source.getFrom().deriveDataPaths(field).getRoots();
        assertEquals(2, roots.size());
        for (DataPathNode root : roots) {
            DataPathNode next = root.getNext().stream().filter(node -> node.getEdge() == target)
                    .findFirst().orElseThrow();
            assertEquals(targetRow, next.getRow());
            assertEquals(root.getTraceId(), next.getTraceId());
        }
    }
}
