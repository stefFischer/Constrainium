package at.sfischer.constraints.data.model;

import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.data.TypePromotionPolicy;
import at.sfischer.constraints.model.Node;

import java.util.*;

public class Graph {

    private final Set<GraphNode> entryNodes = new HashSet<>();

    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final Set<CallRelation> callRelations = new LinkedHashSet<>();
    private final Set<Set<CallRelation>> callRelationGroups = new LinkedHashSet<>();
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

    /** Without an explicit group, relations with the same trace and edge pair are compared together. */
    public void addCallRelation(CallRelation relation) {
        checkMutable();
        validateRelation(relation);
        callRelations.add(relation);
    }

    /** Records comparable observations of one logical call relationship, possibly across executions. */
    public void addCallRelationGroup(Collection<CallRelation> relations) {
        checkMutable();
        Set<CallRelation> group = new LinkedHashSet<>(relations);
        if (group.isEmpty()) {
            return;
        }

        CallRelation first = group.iterator().next();
        for (CallRelation relation : group) {
            validateRelation(relation);
            if (relation.source() != first.source() || relation.target() != first.target()) {
                throw new IllegalArgumentException("Comparable observations must refer to the same pair of edges.");
            }
        }

        callRelationGroups.add(Collections.unmodifiableSet(group));
        callRelations.addAll(group);
    }

    /** Immutable snapshots preserve grouping for inspection or later graph composition. */
    public List<List<CallRelation>> getCallRelationGroups() {
        List<List<CallRelation>> groups = new ArrayList<>();
        Set<CallRelation> explicitlyGrouped = new HashSet<>();
        for (Set<CallRelation> group : callRelationGroups) {
            groups.add(List.copyOf(group));
            explicitlyGrouped.addAll(group);
        }

        Map<DefaultGroup, List<CallRelation>> defaults = new LinkedHashMap<>();
        for (CallRelation relation : callRelations) {
            if (!explicitlyGrouped.contains(relation)) {
                DefaultGroup key = new DefaultGroup(relation.traceId(), relation.source(), relation.target());
                defaults.computeIfAbsent(key, ignored -> new ArrayList<>()).add(relation);
            }
        }

        for (List<CallRelation> group : defaults.values()) {
            groups.add(List.copyOf(group));
        }

        return List.copyOf(groups);
    }

    private void validateRelation(CallRelation relation) {
        validateEdge(relation.source());
        validateEdge(relation.target());
        relation.source().dataRow(relation.traceId(), relation.sourceRow());
        relation.target().dataRow(relation.traceId(), relation.targetRow());
    }

    private record DefaultGroup(String traceId, CallEdge source, CallEdge target) {}

    public void finalizeGraph() {
        finalizeGraph(null);
    }

    /** Derives results once, releases raw evidence, and prevents further graph mutations. */
    public void finalizeGraph(TypePromotionPolicy policy) {
        finalizeGraph(policy, List.of());
    }

    /** Additional Boolean term templates supplement the defaults for every data-flow comparison. */
    public void finalizeGraph(TypePromotionPolicy policy, Collection<? extends Node> additionalTerms) {
        if (finalized) {
            return;
        }

        List<Node> matchingTerms = List.copyOf(additionalTerms);
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

        inferDataFlows(policy, matchingTerms);
        for (Edge edge : allEdges) {
            edge.freeze();
        }
        nodes.values().forEach(GraphNode::freeze);
        calls.forEach(CallEdge::discardData);
        callRelations.clear();
        callRelationGroups.clear();
        traceIds.addAll(ids);
        finalized = true;
    }

    private void inferDataFlows(TypePromotionPolicy policy, Collection<? extends Node> additionalTerms) {
        for (List<CallRelation> group : getCallRelationGroups()) {
            group.getFirst().source().inferDataFlows(group, policy, additionalTerms);
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
