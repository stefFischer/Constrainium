package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.TypePromotionPolicy;

import java.util.*;

public class Graph {

    private final Set<GraphNode> entryNodes = new HashSet<>();

    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final Set<CallRelation> callRelations = new LinkedHashSet<>();
    private final Set<String> traceIds = new LinkedHashSet<>();
    private boolean finalized;

    public GraphNode getOrCreateNode(GraphNode candidate) {
        checkMutable();
        GraphNode node = nodes.computeIfAbsent(
                candidate.getId(),
                id -> {
                    candidate.attachTo(this);
                    return candidate;
                }
        );
        if (!node.getType().equals(candidate.getType())) {
            throw new IllegalArgumentException("Conflicting types for graph node '" + candidate.getId()
                    + "': '" + node.getType() + "' and '" + candidate.getType() + "'");
        }

        return node;
    }

    public Collection<GraphNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public GraphNode getNode(String id){
        return this.nodes.get(id);
    }

    public void addEntryNode(GraphNode candidate){
        GraphNode node = getOrCreateNode(candidate);
        this.entryNodes.add(node);
    }

    public Set<GraphNode> getEntryNodes() {
        return Collections.unmodifiableSet(entryNodes);
    }

    public boolean isFinalized() {
        return finalized;
    }

    private void checkMutable() {
        if (finalized) {
            throw new IllegalStateException("The graph is finalized.");
        }
    }

    public Set<CallRelation> getCallRelations() {
        return Collections.unmodifiableSet(callRelations);
    }

    public Set<String> getTraceIds() {
        Set<String> ids = new LinkedHashSet<>(traceIds);
        for (Edge edge : edges()) {
            if (edge instanceof CallEdge call) {
                ids.addAll(call.getTraceIds());
                for (DataFlow flow : call.getDataFlows()) {
                    ids.add(flow.getTraceId());
                }
            }
        }

        return Collections.unmodifiableSet(ids);
    }

    /** Records evidence without combining samples from different trace IDs. */
    public void addCallRelation(CallRelation relation) {
        checkMutable();
        validateEdge(relation.source());
        validateEdge(relation.target());
        relation.source().dataRow(relation.traceId(), relation.sourceRow());
        relation.target().dataRow(relation.traceId(), relation.targetRow());
        callRelations.add(relation);
    }

    public void finalizeGraph() {
        finalizeGraph(null);
    }

    /** Derives results once, releases raw evidence, and prevents further graph mutations. */
    public void finalizeGraph(TypePromotionPolicy policy) {
        if (finalized) {
            return;
        }

        List<Edge> allEdges = edges();
        for (Edge edge : allEdges) {
            validateEdge(edge);
        }

        Set<String> ids = getTraceIds();
        List<CallEdge> calls = allEdges.stream().filter(CallEdge.class::isInstance).map(CallEdge.class::cast).toList();
        boolean hasSamples = calls.stream().anyMatch(CallEdge::hasData);
        if (hasSamples) {
            calls.forEach(CallEdge::clearDataFlows);
        }
        for (CallEdge edge : calls) {
            edge.deriveSchema(policy);
        }

        inferDataFlows(policy);
        for (Edge edge : allEdges) {
            edge.freeze();
        }
        nodes.values().forEach(GraphNode::freeze);
        calls.forEach(CallEdge::discardData);
        callRelations.clear();
        traceIds.addAll(ids);
        finalized = true;
    }

    private void inferDataFlows(TypePromotionPolicy policy) {
        Map<FlowGroup, List<CallRelation>> groups = new LinkedHashMap<>();
        for (CallRelation relation : callRelations) {
            FlowGroup group = new FlowGroup(relation.traceId(), relation.source(), relation.target());
            groups.computeIfAbsent(group, key -> new ArrayList<>()).add(relation);
        }

        for (Map.Entry<FlowGroup, List<CallRelation>> entry : groups.entrySet()) {
            InOutputDataCollection sourceData = new InOutputDataCollection();
            InOutputDataCollection targetData = new InOutputDataCollection();
            for (CallRelation relation : entry.getValue()) {
                relation.source().appendRowTo(relation.traceId(), relation.sourceRow(), sourceData);
                relation.target().appendRowTo(relation.traceId(), relation.targetRow(), targetData);
            }

            FlowGroup group = entry.getKey();
            group.source().inferDataFlows(group.traceId(), sourceData, group.target(), targetData, policy);
        }
    }

    private List<Edge> edges() {
        Set<Edge> result = new LinkedHashSet<>();
        for (GraphNode node : nodes.values()) {
            result.addAll(node.getOutgoing());
            result.addAll(node.getIncoming());
        }
        return List.copyOf(result);
    }

    private void validateEdge(Edge edge) {
        if (nodes.get(edge.getFrom().getId()) != edge.getFrom() || nodes.get(edge.getTo().getId()) != edge.getTo()) {
            throw new IllegalArgumentException("Edge endpoints must be registered in this graph: " + edge);
        }
    }

    private record FlowGroup(String traceId, CallEdge source, CallEdge target) {}

    public DataPaths deriveDataPaths(String qualifiedName) {
        List<DataPathNode> roots = new ArrayList<>();
        for (GraphNode entryNode : entryNodes) {
            for (Edge edge : entryNode.getOutgoing()) {
                if (!(edge instanceof CallEdge callEdge)) {
                    continue;
                }

                DataSchemaEntry<SimpleDataSchema> entry = callEdge.getSchema().findDataSchemaEntry(qualifiedName);
                if (entry == null) {
                    continue;
                }

                roots.addAll(entryNode.deriveDataPaths(entry).getRoots());
            }
        }

        return new DataPaths(roots);
    }
}
