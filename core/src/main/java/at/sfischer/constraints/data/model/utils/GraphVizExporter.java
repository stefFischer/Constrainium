package at.sfischer.constraints.data.model.utils;

import at.sfischer.constraints.data.model.Edge;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.constraints.data.model.GraphNode;

import java.util.HashSet;
import java.util.Set;

public class GraphVizExporter {

    public static String export(Graph graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");
        for (GraphNode node : graph.getNodes()) {
            sb.append("  \"")
                    .append(node.getId())
                    .append("\" [label=\"")
                    .append(node.getName())
                    .append("\"];\n");
        }

        Set<Edge> exported = new HashSet<>();
        for (GraphNode node : graph.getNodes()) {
            for (Edge edge : node.getOutgoing()) {
                if (!exported.add(edge)) {
                    continue;
                }

                sb.append("  \"")
                        .append(edge.getFrom().getId())
                        .append("\" -> \"")
                        .append(edge.getTo().getId())
                        .append("\";\n");
            }
        }
        sb.append("}");

        return sb.toString();
    }
}