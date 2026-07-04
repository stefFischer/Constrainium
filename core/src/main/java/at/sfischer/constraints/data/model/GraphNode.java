package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;

import java.util.*;

public class GraphNode {
    private final String id;
    private final String name;

    private final List<Edge> incoming = new ArrayList<>();
    private final List<Edge> outgoing = new ArrayList<>();

    public GraphNode(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public void addIncoming(Edge e) { incoming.add(e); }
    public void addOutgoing(Edge e) { outgoing.add(e); }

    public List<Edge> getIncoming() { return Collections.unmodifiableList(incoming); }
    public List<Edge> getOutgoing() { return Collections.unmodifiableList(outgoing); }

    public List<Edge> findOutgoingTo(GraphNode target){
        List<Edge> edges = new ArrayList<>();
        for (Edge edge : outgoing) {
            if(edge.getTo().getId().equals(target.getId())){
                edges.add(edge);
            }
        }

        return edges;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name + ")";
    }

    public DataPaths deriveDataPaths(DataSchemaEntry<SimpleDataSchema> source) {
        List<DataPathNode> roots = new ArrayList<>();
        for (Edge edge : this.getOutgoing()) {
            if (!(edge instanceof CallEdge callEdge)) {
                continue;
            }

            if (contains(callEdge, source)) {
                callEdge.getDataFlows().stream()
                        .map(DataFlow::getTraceId)
                        .distinct()
                        .forEach(traceId ->
                                roots.add(build(callEdge, source, traceId, new HashSet<>()))
                        );

            }
        }

        return new DataPaths(roots);
    }

    private record Visited(
            CallEdge edge,
            DataSchemaEntry<SimpleDataSchema> entry) {
    }

    private DataPathNode build(
            CallEdge edge,
            DataSchemaEntry<SimpleDataSchema> current,
            String traceId,
            Set<Visited> visited) {

        Visited state = new Visited(edge, current);
        if (!visited.add(state)) {
            return new DataPathNode(edge, current);
        }

        DataPathNode node = new DataPathNode(edge, current);
        for (DataFlow flow : edge.getDataFlows()) {
            if (!flow.getFrom().equals(edge)) {
                continue;
            }

            if (!flow.getTraceId().equals(traceId)) {
                continue;
            }

            DataSchemaEntry<SimpleDataSchema> nextEntry = flow.getDataFlows().get(current);
            if (nextEntry == null) {
                continue;
            }

            node.addNext(build(flow.getTo(), nextEntry, traceId, visited));
        }

        return node;
    }

    private boolean contains(CallEdge edge, DataSchemaEntry<SimpleDataSchema> entry) {
        return edge.getDataFlows().stream()
                .anyMatch(df ->
                        df.getDataFlows().containsKey(entry) ||
                        df.getDataFlows().containsValue(entry)
                );
    }
}