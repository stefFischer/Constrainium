package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;

import java.util.*;

public class GraphNode {
    private final String id;
    private final String name;
    private final String type;
    private boolean frozen;
    private Graph graph;

    private final List<Edge> incoming = new ArrayList<>();
    private final List<Edge> outgoing = new ArrayList<>();

    public GraphNode(String id, String name) {
        this(id, name, "unknown");
    }

    public GraphNode(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = Objects.requireNonNull(type, "type");
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }

    public void addIncoming(Edge edge) {
        checkMutable();
        incoming.add(edge);
    }

    public void addOutgoing(Edge edge) {
        checkMutable();
        outgoing.add(edge);
    }

    public boolean isFrozen() {
        return frozen;
    }

    void checkMutable() {
        if (frozen) {
            throw new IllegalStateException("The graph node is finalized.");
        }
    }

    void attachTo(Graph owner) {
        checkMutable();
        if (graph != null && graph != owner) {
            throw new IllegalArgumentException("The node already belongs to another graph.");
        }

        graph = owner;
    }

    void freeze() {
        frozen = true;
    }

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

            Set<Occurrence> occurrences = new LinkedHashSet<>();
            for (DataFlow flow : callEdge.getDataFlows()) {
                if (flow.getFrom() == callEdge && flow.getDataFlows().containsKey(source)) {
                    occurrences.add(new Occurrence(flow.getTraceId(), flow.getSourceRow()));
                }
                if (flow.getTo() == callEdge && flow.getDataFlows().containsValue(source)) {
                    occurrences.add(new Occurrence(flow.getTraceId(), flow.getTargetRow()));
                }
            }
            for (Occurrence occurrence : occurrences) {
                roots.add(build(callEdge, source, occurrence.traceId(), occurrence.row(), new HashSet<>()));
            }
        }

        return new DataPaths(roots);
    }

    private record Occurrence(String traceId, Integer row) {}

    private record Visited(CallEdge edge, DataSchemaEntry<SimpleDataSchema> entry, Integer row) {}

    private DataPathNode build(
            CallEdge edge,
            DataSchemaEntry<SimpleDataSchema> current,
            String traceId,
            Integer row,
            Set<Visited> visited) {

        DataPathNode node = new DataPathNode(edge, current, traceId, row);
        Visited state = new Visited(edge, current, row);
        if (!visited.add(state)) {
            return node;
        }

        for (DataFlow flow : edge.getDataFlows()) {
            if (!flow.getFrom().equals(edge)) {
                continue;
            }

            if (!flow.getTraceId().equals(traceId) || !Objects.equals(flow.getSourceRow(), row)) {
                continue;
            }

            DataSchemaEntry<SimpleDataSchema> nextEntry = flow.getDataFlows().get(current);
            if (nextEntry == null) {
                continue;
            }

            node.addNext(build(flow.getTo(), nextEntry, traceId, flow.getTargetRow(), new HashSet<>(visited)));
        }

        return node;
    }

}
