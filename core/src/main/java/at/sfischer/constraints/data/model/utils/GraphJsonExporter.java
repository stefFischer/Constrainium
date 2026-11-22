package at.sfischer.constraints.data.model.utils;

import at.sfischer.constraints.data.*;
import at.sfischer.constraints.data.model.*;
import at.sfischer.constraints.model.ArrayType;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Versioned JSON snapshot of graph topology, schemas, constraints and inferred data flows. */
public final class GraphJsonExporter {
    private GraphJsonExporter() {}

    public static String export(Graph graph) {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(new Snapshot(graph).document());
    }

    public static void write(Graph graph, Path path) throws IOException {
        Files.writeString(path, export(graph), StandardCharsets.UTF_8);
    }

    private static final class Snapshot {
        private final Graph graph;
        private final List<GraphNode> nodes;
        private final List<Edge> edges = new ArrayList<>();
        private final IdentityHashMap<Edge, String> edgeIds = new IdentityHashMap<>();
        private final IdentityHashMap<DataSchema, String> schemaIds = new IdentityHashMap<>();
        private final IdentityHashMap<DataSchemaEntry<?>, String> entryIds = new IdentityHashMap<>();
        private final IdentityHashMap<DataFlow, String> flowIds = new IdentityHashMap<>();
        private final IdentityHashMap<Object, String> objectIds = new IdentityHashMap<>();
        private final List<Map<String, Object>> schemas = new ArrayList<>();
        private final List<Map<String, Object>> entries = new ArrayList<>();
        private final List<Map<String, Object>> flows = new ArrayList<>();
        private final List<Map<String, Object>> objects = new ArrayList<>();

        Snapshot(Graph graph) {
            this.graph = Objects.requireNonNull(graph, "graph");
            nodes = graph.getNodes().stream().sorted(Comparator.comparing(GraphNode::getId)).toList();
            for (GraphNode node : nodes) {
                if (node.getClass() != GraphNode.class) throw unsupported(node);
                for (Edge edge : node.getOutgoing()) {
                    if (edge instanceof CallEdge call && call.hasData()) {
                        throw new IllegalStateException("Finalize the graph before exporting pending call data.");
                    }
                }
                // Include both directions, but export each edge instance only once.
                for (Edge edge : node.getOutgoing()) registerEdge(edge);
                for (Edge edge : node.getIncoming()) registerEdge(edge);
            }
        }

        private void registerEdge(Edge edge) {
            if (graph.getNode(edge.getFrom().getId()) != edge.getFrom()
                    || graph.getNode(edge.getTo().getId()) != edge.getTo()) {
                throw new IllegalArgumentException("Edge endpoint is not a registered graph node: " + edge);
            }
            if (!edgeIds.containsKey(edge)) {
                edgeIds.put(edge, "edge-" + edges.size());
                edges.add(edge);
            }
        }

        Map<String, Object> document() {
            List<Map<String, Object>> nodeRows = new ArrayList<>();
            for (GraphNode node : nodes) nodeRows.add(row("id", node.getId(), "name", node.getName(), "type", node.getType(),
                    "incoming", node.getIncoming().stream().map(edgeIds::get).toList(),
                    "outgoing", node.getOutgoing().stream().map(edgeIds::get).toList()));
            List<Map<String, Object>> edgeRows = new ArrayList<>();
            // Register every schema first; data flows then reference exact entry identities.
            for (Edge edge : edges) if (edge instanceof CallEdge call) schema(call.getSchema());
            for (Edge edge : edges) {
                String kind;
                if (edge.getClass() == DatabaseQueryEdge.class) kind = "database-query";
                else if (edge.getClass() == SynchronousCallEdge.class) kind = "synchronous-call";
                else if (edge.getClass() == AsynchronousCallEdge.class) kind = "asynchronous-call";
                else if (edge.getClass() == DependencyEdge.class) kind = "dependency";
                else throw unsupported(edge);
                Map<String, Object> record = row("id", edgeIds.get(edge), "kind", kind,
                        "from", edge.getFrom().getId(), "to", edge.getTo().getId());
                if (edge instanceof CallEdge call) {
                    record.put("schema", schema(call.getSchema()));
                    record.put("dataFlows", call.getDataFlows().stream().map(this::flow).toList());
                }
                if (edge instanceof DatabaseQueryEdge query) record.put("query", query.getQuery());
                edgeRows.add(record);
            }
            return row("format", "constrainium-call-graph", "version", 1,
                    "finalized", graph.isFinalized(), "traceIds", graph.getTraceIds().stream().sorted().toList(),
                    "entryNodes", graph.getEntryNodes().stream().map(GraphNode::getId).sorted().toList(),
                    "nodes", nodeRows, "edges", edgeRows, "schemas", schemas,
                    "schemaEntries", entries, "dataFlows", flows, "objects", objects);
        }

        private String schema(DataSchema schema) {
            if (schema == null) return null;
            if (schemaIds.containsKey(schema)) return schemaIds.get(schema);
            String id = "schema-" + schemaIds.size();
            schemaIds.put(schema, id);
            Map<String, Object> record = row("id", id);
            schemas.add(record);
            if (schema.getClass() == SimpleDataSchema.class) {
                SimpleDataSchema simple = (SimpleDataSchema) schema;
                record.put("kind", "simple");
                record.put("entries", simple.getSchemaEntryNames().stream().sorted()
                        .map(name -> entry(simple.getSchemaEntry(name))).toList());
            } else if (schema.getClass() == InOutputDataSchema.class) {
                InOutputDataSchema<?> inOut = (InOutputDataSchema<?>) schema;
                record.put("kind", "input-output");
                record.put("inputPrefix", inOut.getInputPrefix());
                record.put("outputPrefix", inOut.getOutputPrefix());
                record.put("input", schema(inOut.getInputSchema()));
                record.put("output", schema(inOut.getOutputSchema()));
            } else throw unsupported(schema);
            record.put("parentEntry", entry(schema.getParentEntry()));
            return id;
        }

        private String entry(DataSchemaEntry<?> entry) {
            if (entry == null) return null;
            if (entryIds.containsKey(entry)) return entryIds.get(entry);
            String id = "field-" + entryIds.size();
            entryIds.put(entry, id);
            Map<String, Object> record = row("id", id, "name", entry.name,
                    "qualifiedName", entry.getQualifiedName(), "type", type(entry.type), "mandatory", entry.mandatory);
            entries.add(record);
            record.put("parentSchema", schema(entry.getParentSchema()));
            record.put("nestedSchema", schema(entry.dataSchema));
            record.put("constraints", entry.constraints.stream().map(this::value).toList());
            record.put("potentialConstraints", entry.potentialConstraints.stream().map(this::value).toList());
            return id;
        }

        private String flow(DataFlow flow) {
            if (flowIds.containsKey(flow)) return flowIds.get(flow);
            String id = "flow-" + flowIds.size();
            flowIds.put(flow, id);
            String from = edgeIds.get(flow.getFrom()), to = edgeIds.get(flow.getTo());
            if (from == null || to == null) throw new IllegalArgumentException("Data flow references an edge outside this graph");
            List<Map<String, Object>> mappings = new ArrayList<>();
            for (var mapping : flow.getDataFlows().entrySet()) {
                if (!entryIds.containsKey(mapping.getKey()) || !entryIds.containsKey(mapping.getValue())) {
                    throw new IllegalArgumentException("Data flow references a field outside the graph's edge schemas");
                }
                mappings.add(row("from", entryIds.get(mapping.getKey()), "to", entryIds.get(mapping.getValue())));
            }
            mappings.sort(Comparator.comparing(mapping -> mapping.get("from").toString()));
            flows.add(row("id", id, "traceId", flow.getTraceId(), "from", from, "to", to,
                    "sourceRow", flow.getSourceRow(), "targetRow", flow.getTargetRow(), "mappings", mappings));
            return id;
        }

        private Object type(Type type) {
            if (type instanceof TypeEnum primitive) return row("kind", primitive.name());
            if (type instanceof ArrayType array) return row("kind", "ARRAY", "elementType", type(array.elementType()));
            throw unsupported(type);
        }

        /**
         * Constraints and their expression/policy objects are stored as typed state,
         * rather than toString() or evaluated getters. Identity references preserve
         * shared expressions and cycles in provenance without recursive expansion.
         */
        private Object value(Object value) {
            if (value == null || value instanceof String || value instanceof Boolean) return value;
            if (value instanceof Character character) return row("javaType", "char", "value", character.toString());
            if (value instanceof Number number) {
                // Strings preserve 64-bit integers and non-finite floating values in JS consumers.
                return row("javaType", number.getClass().getName(), "value", number.toString());
            }
            if (value instanceof Enum<?> enumeration) return row("javaType", enumeration.getDeclaringClass().getName(), "value", enumeration.name());
            if (value instanceof Type type) return type(type);
            if (value instanceof DataSchema schema) return row("schemaRef", schema(schema));
            if (value instanceof DataSchemaEntry<?> entry) return row("fieldRef", entry(entry));
            if (objectIds.containsKey(value)) return row("$ref", objectIds.get(value));
            String id = "object-" + objectIds.size();
            objectIds.put(value, id);
            Class<?> clazz = value.getClass();
            Map<String, Object> record = row("id", id, "javaType", clazz.getName());
            objects.add(record);
            if (value instanceof Map<?, ?> map) {
                List<Object> items = new ArrayList<>();
                for (var item : map.entrySet()) items.add(row("key", value(item.getKey()), "value", value(item.getValue())));
                record.put("entries", items);
            } else if (value instanceof Collection<?> collection) {
                record.put("items", collection.stream().map(this::value).toList());
            } else if (clazz.isArray()) {
                List<Object> items = new ArrayList<>();
                for (int i = 0; i < Array.getLength(value); i++) items.add(value(Array.get(value, i)));
                record.put("items", items);
            } else {
                if (!clazz.getName().startsWith("at.sfischer.constraints.")) throw unsupported(value);
                Map<String, Object> fields = new TreeMap<>();
                for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
                    Field[] declared = current.getDeclaredFields();
                    Arrays.sort(declared, Comparator.comparing(Field::getName));
                    for (Field field : declared) {
                        if (Modifier.isStatic(field.getModifiers())) continue;
                        try {
                            if (!field.trySetAccessible()) throw unsupported(value);
                            fields.put(current.getName() + "#" + field.getName(), value(field.get(value)));
                        } catch (IllegalAccessException e) {
                            throw new IllegalArgumentException("Cannot serialize " + field, e);
                        }
                    }
                }
                record.put("fields", fields);
            }
            return row("$ref", id);
        }
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    private static IllegalArgumentException unsupported(Object value) {
        return new IllegalArgumentException("Unsupported graph serialization type: " + (value == null ? "null" : value.getClass().getName()));
    }
}
