package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class GraphSerializer {

    public static String serializeGraph(Collection<GraphNode> nodes) {
        StringBuilder sb = new StringBuilder();
        serializeNodes(nodes, sb);
        serializeEdges(nodes, sb);
        serializeDataFlows(nodes, sb);
        return sb.toString();
    }

    private static void serializeNodes(Collection<GraphNode> nodes, StringBuilder sb) {
        sb.append("[NODES]\n");
        for (GraphNode node : nodes) {
            sb.append("- ")
                    .append(node.getId())
                    .append(": ")
                    .append(node.getName())
                    .append("\n");
        }
        sb.append("\n");
    }

    private static void serializeEdges(Collection<GraphNode> nodes, StringBuilder sb) {
        sb.append("[EDGES]\n");

        Set<Edge> visited = new HashSet<>();

        for (GraphNode node : nodes) {
            for (Edge edge : node.getOutgoing()) {
                if (!visited.add(edge)) continue;

                if (edge instanceof CallEdge callEdge) {
                    sb.append("- CallEdge: ")
                            .append(formatNode(edge.getFrom()))
                            .append(" -> ")
                            .append(formatNode(edge.getTo()))
                            .append("\n");

                    sb.append("  Schema:\n");
                    indent(sb, callEdge.getSchema().toString(), 4);

                } else if (edge instanceof DependencyEdge) {
                    sb.append("- DependencyEdge: ")
                            .append(formatNode(edge.getFrom()))
                            .append(" -> ")
                            .append(formatNode(edge.getTo()))
                            .append("\n");
                }
            }
        }

        sb.append("\n");
    }

    private static void serializeDataFlows(Collection<GraphNode> nodes, StringBuilder sb) {
        sb.append("[DATA FLOWS]\n");
        for (GraphNode node : nodes) {
            for (DataFlow flow : node.getDataFlows()) {
                sb.append("- Flow: ")
                        .append(formatEdge(flow.getFrom()))
                        .append(" ==> ")
                        .append(formatEdge(flow.getTo()))
                        .append("\n");

                Map<DataSchemaEntry<SimpleDataSchema>, DataSchemaEntry<SimpleDataSchema>> mappings =
                        flow.getDataFlows();

                if (mappings != null && !mappings.isEmpty()) {
                    sb.append("  Mappings:\n");
                    for (Map.Entry<DataSchemaEntry<SimpleDataSchema>, DataSchemaEntry<SimpleDataSchema>> entry : mappings.entrySet()) {

                        String fromField = flow.getFrom().getFrom().getId() + "." + formatSchemaEntry(entry.getKey());
                        String toField = flow.getTo().getTo().getId() + "." + formatSchemaEntry(entry.getValue());
                        sb.append("    ")
                                .append(fromField)
                                .append(" -> ")
                                .append(toField)
                                .append("\n");
                    }

                } else {
                    sb.append("  (no data flow mappings)\n");
                }
            }
        }

        sb.append("\n");
    }

    private static String formatSchemaEntry(DataSchemaEntry<SimpleDataSchema> entry) {
        return entry.getQualifiedName();
    }

    private static String formatNode(GraphNode node) {
        return node.getName() + "(" + node.getId() + ")";
    }

    private static String formatEdge(CallEdge edge) {
        return formatNode(edge.getFrom()) + " -> " + formatNode(edge.getTo());
    }

    private static void indent(StringBuilder sb, String text, int spaces) {
        String indent = " ".repeat(spaces);
        for (String line : text.split("\n")) {
            sb.append(indent).append(line).append("\n");
        }
    }
}
