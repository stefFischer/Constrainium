package at.sfischer.constraints.data.model.utils;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintTemplate;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.data.model.*;
import at.sfischer.constraints.model.ArrayType;
import at.sfischer.constraints.model.StringLiteral;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.miner.MinApplicationsPolicy;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GraphJsonExporterTest {
    @Test
    void preservesTopologyParallelEdgesCyclesAndEscapedLabels() {
        Graph graph = new Graph();
        GraphNode first = graph.getOrCreateNode(new GraphNode("a", "Grün \"service\"\n", "service"));
        GraphNode second = graph.getOrCreateNode(new GraphNode("b", "database", "database"));
        graph.getOrCreateNode(new GraphNode("isolated", "isolated"));
        graph.addEntryNode(first);
        graph.addEntryNode(second);
        var schema = new InOutputDataSchema<>(new SimpleDataSchema(), new SimpleDataSchema());
        new SynchronousCallEdge(first, second, schema);
        new DatabaseQueryEdge(first, second, schema, "SELECT \"name\" FROM products WHERE sku = ?");
        new DependencyEdge(second, first);
        new AsynchronousCallEdge(first, first, new SimpleDataSchema());

        String serialized = GraphJsonExporter.export(graph);
        JSONObject document = new JSONObject(serialized);
        assertEquals("constrainium-call-graph", document.getString("format"));
        assertEquals(1, document.getInt("version"));
        assertEquals(3, document.getJSONArray("nodes").length());
        assertEquals(2, document.getJSONArray("entryNodes").length());
        Map<String, JSONObject> nodes = index(document.getJSONArray("nodes"));
        assertEquals(first.getName(), nodes.get("a").getString("name"));
        assertEquals("service", nodes.get("a").getString("type"));
        assertEquals("database", nodes.get("b").getString("type"));
        assertEquals("unknown", nodes.get("isolated").getString("type"));
        assertEquals(3, nodes.get("a").getJSONArray("outgoing").length());
        assertEquals(4, document.getJSONArray("edges").length());
        Set<String> kinds = new HashSet<>();
        for (Object object : document.getJSONArray("edges")) {
            JSONObject edge = (JSONObject) object;
            kinds.add(edge.getString("kind"));
            assertTrue(nodes.containsKey(edge.getString("from")));
            assertTrue(nodes.containsKey(edge.getString("to")));
            if (edge.has("query")) assertEquals("SELECT \"name\" FROM products WHERE sku = ?", edge.getString("query"));
        }
        assertEquals(Set.of("synchronous-call", "database-query", "dependency", "asynchronous-call"), kinds);
        assertEquals(serialized, GraphJsonExporter.export(graph));
    }

    @Test
    void preservesNestedSchemasCustomPrefixesTypesAndConstraints() {
        SimpleDataSchema input = new SimpleDataSchema();
        var products = input.objectArrayEntry("products", false);
        var sku = products.dataSchema.stringEntry("sku", true);
        products.dataSchema.arrayEntryFor(new ArrayType(TypeEnum.INTEGER), "matrix", true);
        sku.constraints.add(new Constraint(new StringEquals(new Variable("sku"), new StringLiteral("HAY-002"))));
        sku.potentialConstraints.add(new Constraint(new StringEquals(new Variable("sku"), new StringLiteral("STRW-001"))));
        var schema = new InOutputDataSchema<>(input, new SimpleDataSchema(), "request", "response");
        Graph graph = new Graph();
        GraphNode node = graph.getOrCreateNode(new GraphNode("node", "node"));
        new SynchronousCallEdge(node, node, schema);
        JSONObject document = new JSONObject(GraphJsonExporter.export(graph));
        Map<String, JSONObject> schemas = index(document.getJSONArray("schemas"));
        Map<String, JSONObject> entries = index(document.getJSONArray("schemaEntries"));
        JSONObject inOut = schemas.values().stream().filter(s -> s.getString("kind").equals("input-output")).findFirst().orElseThrow();
        assertEquals("request", inOut.getString("inputPrefix"));
        assertEquals("response", inOut.getString("outputPrefix"));
        JSONObject productEntry = named(entries, "products");
        assertFalse(productEntry.getBoolean("mandatory"));
        assertEquals("ARRAY", productEntry.getJSONObject("type").getString("kind"));
        assertEquals("COMPLEXTYPE", productEntry.getJSONObject("type").getJSONObject("elementType").getString("kind"));
        assertTrue(schemas.containsKey(productEntry.getString("nestedSchema")));
        JSONObject matrix = named(entries, "matrix");
        assertEquals("INTEGER", matrix.getJSONObject("type").getJSONObject("elementType")
                .getJSONObject("elementType").getString("kind"));
        JSONObject skuEntry = named(entries, "sku");
        assertEquals("request.products.sku", skuEntry.getString("qualifiedName"));
        assertEquals(1, skuEntry.getJSONArray("constraints").length());
        assertEquals(1, skuEntry.getJSONArray("potentialConstraints").length());
        Map<String, JSONObject> objects = index(document.getJSONArray("objects"));
        JSONObject constraint = objects.get(skuEntry.getJSONArray("constraints").getJSONObject(0).getString("$ref"));
        assertEquals(Constraint.class.getName(), constraint.getString("javaType"));
        JSONObject expression = objects.get(constraint.getJSONObject("fields")
                .getJSONObject(Constraint.class.getName() + "#term").getString("$ref"));
        assertEquals(StringEquals.class.getName(), expression.getString("javaType"));
        assertTrue(document.toString().contains("HAY-002"));
        assertTrue(document.toString().contains("STRW-001"));
        // Parent links point back to the existing schema/entry rather than expanding cycles.
        assertEquals(productEntry.getString("id"), schemas.get(productEntry.getString("nestedSchema")).getString("parentEntry"));
    }

    @Test
    void preservesDataFlowOwnershipAndExactEntryIdentityAcrossEqualSchemas() {
        Graph graph = new Graph();
        GraphNode node = graph.getOrCreateNode(new GraphNode("node", "node"));
        SimpleDataSchema firstSchema = new SimpleDataSchema(), secondSchema = new SimpleDataSchema();
        var firstField = firstSchema.stringEntry("id", true);
        var secondField = secondSchema.stringEntry("id", true);
        AsynchronousCallEdge first = new AsynchronousCallEdge(node, node, firstSchema);
        AsynchronousCallEdge second = new AsynchronousCallEdge(node, node, secondSchema);
        DataFlow flow = new DataFlow("trace-123", first, second);
        flow.getDataFlows().put(firstField, secondField);
        // Ownership and flow direction need not agree; serialize both explicitly.
        second.getDataFlows().add(flow);
        first.getDataFlows().add(flow);
        JSONObject document = new JSONObject(GraphJsonExporter.export(graph));
        JSONArray flows = document.getJSONArray("dataFlows");
        assertEquals(1, flows.length());
        JSONObject serialized = flows.getJSONObject(0);
        assertEquals("trace-123", serialized.getString("traceId"));
        assertNotEquals(serialized.getString("from"), serialized.getString("to"));
        JSONObject mapping = serialized.getJSONArray("mappings").getJSONObject(0);
        assertNotEquals(mapping.getString("from"), mapping.getString("to"));
        Map<String, JSONObject> entries = index(document.getJSONArray("schemaEntries"));
        assertEquals("id", entries.get(mapping.getString("from")).getString("qualifiedName"));
        assertEquals("id", entries.get(mapping.getString("to")).getString("qualifiedName"));
        for (Object edge : document.getJSONArray("edges")) {
            assertEquals(serialized.getString("id"), ((JSONObject) edge).getJSONArray("dataFlows").getString(0));
        }
    }

    @Test
    void preservesConstraintProvenanceSharedExpressionsAndExactNumericLiterals() {
        var term = new EqualOperator(new Variable("id"), new NumberLiteral(9007199254740993L));
        var origin = new ConstraintTemplate("id equality", term, new MinApplicationsPolicy(10));
        SimpleDataSchema schema = new SimpleDataSchema();
        var field = schema.numberEntry("id", true);
        field.constraints.add(new Constraint(term, origin));
        Graph graph = new Graph();
        GraphNode node = graph.getOrCreateNode(new GraphNode("node", "node"));
        new AsynchronousCallEdge(node, node, schema);
        JSONObject document = new JSONObject(GraphJsonExporter.export(graph));
        Map<String, JSONObject> objects = index(document.getJSONArray("objects"));
        JSONObject constraint = objects.values().stream().filter(object -> object.getString("javaType")
                .equals(Constraint.class.getName())).findFirst().orElseThrow();
        JSONObject fields = constraint.getJSONObject("fields");
        String expressionId = fields.getJSONObject(Constraint.class.getName() + "#term").getString("$ref");
        JSONObject template = objects.get(fields.getJSONObject(Constraint.class.getName() + "#derivedFrom").getString("$ref"));
        assertEquals("id equality", template.getJSONObject("fields").getString(ConstraintTemplate.class.getName() + "#name"));
        assertEquals(expressionId, template.getJSONObject("fields").getJSONObject(ConstraintTemplate.class.getName() + "#term").getString("$ref"));
        JSONObject policy = objects.get(template.getJSONObject("fields")
                .getJSONObject(ConstraintTemplate.class.getName() + "#retentionPolicy").getString("$ref"));
        assertEquals("10", policy.getJSONObject("fields").getJSONObject(MinApplicationsPolicy.class.getName() + "#minNumberOfValidData").getString("value"));
        assertTrue(document.toString().contains("\"value\":\"9007199254740993\""));
    }

    @Test
    void writesUtf8AndSupportsEmptyGraphs(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("graph.json");
        Graph empty = new Graph();
        GraphJsonExporter.write(empty, file);
        JSONObject document = new JSONObject(Files.readString(file, StandardCharsets.UTF_8));
        for (String table : List.of("nodes", "edges", "schemas", "schemaEntries", "dataFlows", "objects", "entryNodes")) {
            assertEquals(0, document.getJSONArray(table).length());
        }
        Graph graph = new Graph();
        graph.addEntryNode(new GraphNode("ä", "Grün"));
        GraphJsonExporter.write(graph, file);
        assertEquals("Grün", new JSONObject(Files.readString(file, StandardCharsets.UTF_8))
                .getJSONArray("nodes").getJSONObject(0).getString("name"));
    }

    @Test
    void rejectsDanglingReferencesAndUnknownEdgeTypesRatherThanLosingInformation() {
        Graph graph = new Graph();
        GraphNode node = graph.getOrCreateNode(new GraphNode("a", "a"));
        new DependencyEdge(node, new GraphNode("unregistered", "unregistered"));
        assertThrows(IllegalArgumentException.class, () -> GraphJsonExporter.export(graph));
        Graph custom = new Graph();
        GraphNode registered = custom.getOrCreateNode(new GraphNode("b", "b"));
        new Edge(registered, registered) {};
        assertThrows(IllegalArgumentException.class, () -> GraphJsonExporter.export(custom));
    }

    private Map<String, JSONObject> index(JSONArray array) {
        Map<String, JSONObject> result = new HashMap<>();
        for (Object object : array) {
            JSONObject record = (JSONObject) object;
            assertNull(result.put(record.getString("id"), record), "Duplicate serialized ID");
        }
        return result;
    }

    private JSONObject named(Map<String, JSONObject> entries, String name) {
        return entries.values().stream().filter(entry -> entry.getString("name").equals(name)).findFirst().orElseThrow();
    }
}
