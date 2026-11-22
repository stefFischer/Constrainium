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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseQueryFragmentTest {

    @Test
    void databaseFragmentsRetainQueryOrOperationOnDatabaseQueryEdges() {
        AbstractSpan root = new AbstractSpan("POST /checkout", "server");
        root.getAttributes().put("service.name", "checkout-service");
        root.getAttributes().put("http.request.method", "POST");
        root.getAttributes().put("http.route", "/checkout");
        root.setSpanData(new SpanData(new DataObject(), new DataObject()));
        root.addChild(database("REDIS", "EXISTS"));
        root.addChild(database("REDIS", "HGET"));
        AbstractSpan categoryQuery = database("MONGO", "find");
        categoryQuery.getAttributes().put("db.query.text", "find products by category");
        categoryQuery.getAttributes().put("db.statement", "legacy query");
        root.addChild(categoryQuery);
        AbstractSpan skuQuery = database("MONGO", "find");
        skuQuery.getAttributes().put("db.statement", "find products by sku");
        root.addChild(skuQuery);
        root.addChild(database("MONGO", "findAndModify"));
        root.addChild(database("REDIS", "HGET"));
        AbstractSpan repeatedCategoryQuery = database("MONGO", "find");
        repeatedCategoryQuery.getAttributes().put("db.statement", "find products by category");
        root.addChild(repeatedCategoryQuery);
        CallGraphBuilder builder = new CallGraphBuilder(new DispatchingSpanFragmentBuilder(
                new TraceonSpanFragmentBuilders.HttpServer(),
                new TraceonSpanFragmentBuilders.Database("REDIS"),
                new TraceonSpanFragmentBuilders.Database("MONGO")));

        Graph graph = builder.buildMutable(new ClusteredAbstractSpan(root), List.of("checkout"));

        GraphNode caller = graph.getNodes().stream()
                .filter(node -> node.getName().equals("checkout-service")).findFirst().orElseThrow();
        List<DatabaseQueryEdge> queries = caller.getOutgoing().stream()
                .map(edge -> assertInstanceOf(DatabaseQueryEdge.class, edge)).toList();
        assertEquals(List.of("EXISTS", "HGET", "find products by category", "find products by sku", "findAndModify"),
                queries.stream().map(DatabaseQueryEdge::getQuery).toList());
        assertTrue(queries.stream().allMatch(CallEdge::hasData));
        assertSame(queries.get(2).getTo(), queries.get(3).getTo());
        assertEquals(2, queries.get(1).getDataByTrace().get("checkout").size());
        assertEquals(2, queries.get(2).getDataByTrace().get("checkout").size());

        graph.finalizeGraph();

        String snapshot = GraphJsonExporter.export(graph);
        assertTrue(snapshot.contains("database-query"));
        for (DatabaseQueryEdge query : queries) {
            assertTrue(snapshot.contains(query.getQuery()));
            assertTrue(query.isFrozen());
            assertTrue(query.getDataByTrace().isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MONGO", "REDIS"})
    void repeatedDatabaseCallsShareAnEdgeAndPreserveSeparateMappingsForEachOccurrenceAndTrace(String kind) {
        ClusteredAbstractSpan cluster = new ClusteredAbstractSpan(addressLookups(kind, "a"));
        cluster.addSpan(addressLookups(kind, "b"));
        Graph graph = builder().buildMutable(cluster, List.of("trace-a", "trace-b"));
        GraphNode caller = service(graph, "checkout-service");
        assertEquals(1, caller.getOutgoing().size());
        DatabaseQueryEdge query = assertInstanceOf(DatabaseQueryEdge.class, caller.getOutgoing().getFirst());
        CallEdge request = (CallEdge) caller.getIncoming().getFirst();
        assertEquals(kind.equals("MONGO") ? "find" : "HGET", query.getQuery());
        assertEquals(Set.of("trace-a", "trace-b"), query.getDataByTrace().keySet());
        assertEquals(2, query.getDataByTrace().get("trace-a").size());
        assertEquals(2, query.getDataByTrace().get("trace-b").size());
        String inputPath = kind.equals("MONGO") ? "payload.filter.id" : "commands.key";
        String outputPath = kind.equals("MONGO") ? "payload.name" : "commands.result";
        assertEquals("B-a", query.getDataByTrace().get("trace-a").getInputDataCollection()
                .getDataCollection().getFirst().getValues(inputPath).getFirst().getValue());
        assertEquals("S-b", query.getDataByTrace().get("trace-b").getInputDataCollection()
                .getDataCollection().getLast().getValues(inputPath).getFirst().getValue());

        graph.finalizeGraph();

        assertEquals(4, request.getDataFlowsTo(query).size());
        assertEquals(4, query.getDataFlowsTo(request).size());
        for (String traceId : List.of("trace-a", "trace-b")) {
            DataFlow billing = flow(request, query, traceId, 0, 0);
            DataFlow shipping = flow(request, query, traceId, 0, 1);
            assertMapping(billing, "input.billingId", "input." + inputPath);
            assertMapping(shipping, "input.shippingId", "input." + inputPath);
            assertFalse(billing.getDataFlows().containsKey(request.getSchema().findDataSchemaEntry("input.shippingId")));
            assertFalse(shipping.getDataFlows().containsKey(request.getSchema().findDataSchemaEntry("input.billingId")));
            assertMapping(flow(query, request, traceId, 0, 0), "output." + outputPath, "output.billingName");
            assertMapping(flow(query, request, traceId, 1, 0), "output." + outputPath, "output.shippingName");
        }
        List<DataPathNode> billingPaths = graph.deriveDataPaths("input.billingId").getRoots();
        assertEquals(2, billingPaths.size());
        assertEquals(Set.of("trace-a", "trace-b"), billingPaths.stream()
                .map(DataPathNode::getTraceId).collect(Collectors.toSet()));
        for (DataPathNode path : billingPaths) {
            assertEquals(1, path.getNext().size());
            DataPathNode lookup = path.getNext().getFirst();
            assertSame(query, lookup.getEdge());
            assertEquals(0, lookup.getRow());
            assertEquals(path.getTraceId(), lookup.getTraceId());
        }
        assertTrue(query.isFrozen());
        assertTrue(query.getDataByTrace().isEmpty());
        assertTrue(graph.getCallRelations().isEmpty());
        assertDoesNotThrow(() -> GraphJsonExporter.export(graph));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MONGO", "REDIS"})
    void aMergedDatabaseCallCanConsumeTheEarlierCallsResponse(String kind) {
        AbstractSpan root = server("checkout-service", "{}", "{}");
        root.addChild(addressLookup(kind, "A", "B"));
        root.addChild(addressLookup(kind, "B", "C"));
        Graph graph = builder().buildMutable(new ClusteredAbstractSpan(root), List.of("trace"));
        GraphNode caller = service(graph, "checkout-service");
        assertEquals(1, caller.getOutgoing().size());
        DatabaseQueryEdge query = assertInstanceOf(DatabaseQueryEdge.class, caller.getOutgoing().getFirst());

        graph.finalizeGraph();

        assertEquals(1, query.getDataFlowsTo(query).size());
        String inputPath = kind.equals("MONGO") ? "input.payload.filter.id" : "input.commands.key";
        String outputPath = kind.equals("MONGO") ? "output.payload.name" : "output.commands.result";
        assertMapping(flow(query, query, "trace", 0, 1), outputPath, inputPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MONGO", "REDIS"})
    void theSameQueryOnDifferentDatabaseNodesRemainsSeparate(String kind) {
        AbstractSpan root = server("checkout-service", "{}", "{}");
        for (String databaseName : List.of("first-db", "second-db", "first-db", "second-db")) {
            AbstractSpan lookup = database(kind, "lookup");
            lookup.getAttributes().put("db.name", databaseName);
            root.addChild(lookup);
        }

        Graph graph = builder().buildMutable(new ClusteredAbstractSpan(root), List.of("trace"));

        List<DatabaseQueryEdge> queries = service(graph, "checkout-service").getOutgoing().stream()
                .map(edge -> assertInstanceOf(DatabaseQueryEdge.class, edge)).toList();
        assertEquals(2, queries.size());
        assertNotSame(queries.getFirst().getTo(), queries.getLast().getTo());
        for (DatabaseQueryEdge query : queries) {
            assertEquals("lookup", query.getQuery());
            assertEquals(2, query.getDataByTrace().get("trace").size());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"MONGO", "REDIS"})
    void theSameQueryFromDifferentCallersRemainsSeparate(String kind) {
        AbstractSpan first = server("first-service", "{}", "{}");
        AbstractSpan second = server("second-service", "{}", "{}");
        first.addChild(database(kind, "lookup"));
        first.addChild(database(kind, "lookup"));
        second.addChild(database(kind, "lookup"));
        second.addChild(database(kind, "lookup"));
        first.addChild(second);

        Graph graph = builder().buildMutable(new ClusteredAbstractSpan(first), List.of("trace"));

        DatabaseQueryEdge firstQuery = service(graph, "first-service").getOutgoing().stream()
                .filter(DatabaseQueryEdge.class::isInstance).map(DatabaseQueryEdge.class::cast)
                .findFirst().orElseThrow();
        DatabaseQueryEdge secondQuery = assertInstanceOf(DatabaseQueryEdge.class,
                service(graph, "second-service").getOutgoing().getFirst());
        assertNotSame(firstQuery, secondQuery);
        assertSame(firstQuery.getTo(), secondQuery.getTo());
        assertEquals(2, firstQuery.getDataByTrace().get("trace").size());
        assertEquals(2, secondQuery.getDataByTrace().get("trace").size());
    }

    private static AbstractSpan addressLookups(String kind, String suffix) {
        AbstractSpan root = server("checkout-service",
                "{billingId:'B-%s',shippingId:'S-%s'}".formatted(suffix, suffix),
                "{billingName:'Billing-%s',shippingName:'Shipping-%s'}".formatted(suffix, suffix));
        root.addChild(addressLookup(kind, "B-" + suffix, "Billing-" + suffix));
        root.addChild(addressLookup(kind, "S-" + suffix, "Shipping-" + suffix));
        return root;
    }

    private static AbstractSpan addressLookup(String kind, String id, String name) {
        AbstractSpan lookup = database(kind, kind.equals("MONGO") ? "find" : "HGET");
        String input = kind.equals("MONGO")
                ? "{payload:{find:'addresses',filter:{id:'%s'}}}".formatted(id)
                : "{commands:[{command:'HGET',key:'%s',field:'name'}]}".formatted(id);
        String output = kind.equals("MONGO")
                ? "{payload:{name:'%s'}}".formatted(name)
                : "{commands:[{command:'HGET',key:'%s',field:'name',result:'%s'}]}".formatted(id, name);
        lookup.setSpanData(new SpanData(DataObject.parseData(input), DataObject.parseData(output)));
        return lookup;
    }

    private static AbstractSpan server(String serviceName, String input, String output) {
        AbstractSpan span = new AbstractSpan("POST /addresses", "server");
        span.getAttributes().put("service.name", serviceName);
        span.getAttributes().put("http.request.method", "POST");
        span.getAttributes().put("http.route", "/addresses");
        span.setSpanData(new SpanData(DataObject.parseData(input), DataObject.parseData(output)));
        return span;
    }

    private static CallGraphBuilder builder() {
        return new CallGraphBuilder(new DispatchingSpanFragmentBuilder(
                new TraceonSpanFragmentBuilders.HttpServer(),
                new TraceonSpanFragmentBuilders.Database("REDIS"),
                new TraceonSpanFragmentBuilders.Database("MONGO")));
    }

    private static GraphNode service(Graph graph, String name) {
        return graph.getNodes().stream().filter(node -> node.getName().equals(name)).findFirst().orElseThrow();
    }

    private static DataFlow flow(CallEdge source, CallEdge target, String traceId, int sourceRow, int targetRow) {
        return source.getDataFlowsTo(target).stream()
                .filter(flow -> flow.getTraceId().equals(traceId)
                        && Integer.valueOf(sourceRow).equals(flow.getSourceRow())
                        && Integer.valueOf(targetRow).equals(flow.getTargetRow()))
                .findFirst().orElseThrow();
    }

    private static void assertMapping(DataFlow flow, String sourcePath, String targetPath) {
        DataSchemaEntry<SimpleDataSchema> source = flow.getFrom().getSchema().findDataSchemaEntry(sourcePath);
        DataSchemaEntry<SimpleDataSchema> target = flow.getTo().getSchema().findDataSchemaEntry(targetPath);
        assertNotNull(source);
        assertNotNull(target);
        assertSame(target, flow.getDataFlows().get(source));
    }

    private static AbstractSpan database(String kind, String operation) {
        AbstractSpan span = new AbstractSpan(operation, kind);
        span.getAttributes().put("db.name", "database");
        span.getAttributes().put("db.operation.name", operation);
        span.setSpanData(new SpanData(new DataObject(), new DataObject()));
        return span;
    }
}
