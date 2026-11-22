package at.sfischer.traces.otel.graph;

import at.sfischer.constraints.data.TypePromotionPolicy;
import at.sfischer.constraints.data.model.CallEdge;
import at.sfischer.constraints.data.model.Edge;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.constraints.data.model.CallRelation;
import at.sfischer.traces.otel.dataextraction.SpanData;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;

public class CallGraphBuilder {

    private final SpanFragmentBuilder fragmentBuilder;

    public CallGraphBuilder(SpanFragmentBuilder fragmentBuilder) {
        this.fragmentBuilder = fragmentBuilder;
    }

    public Graph build(ClusteredAbstractSpan root) {
        return build(root, null);
    }

    public Graph build(ClusteredAbstractSpan root, TypePromotionPolicy typePromotionPolicy) {
        Graph graph = buildMutable(root);
        graph.finalizeGraph(typePromotionPolicy);
        return graph;
    }

    public Graph buildMutable(ClusteredAbstractSpan root) {
        List<String> traceIds = new ArrayList<>();
        for (int index = 0; index < root.getSpanData().size(); index++) {
            traceIds.add(UUID.randomUUID().toString());
        }

        return buildMutable(root, traceIds);
    }

    /** Caller-provided IDs preserve execution or situation boundaries for later graph composition. */
    public Graph buildMutable(ClusteredAbstractSpan root, List<String> traceIds) {
        if (traceIds.size() != root.getSpanData().size()) {
            throw new IllegalArgumentException("Provide one trace ID for each clustered execution.");
        }

        Graph graph = new Graph();
        Map<ClusteredAbstractSpan, GraphFragment> fragmentMap = new IdentityHashMap<>();
        Map<EdgeSpan, List<Integer>> dataRows = new HashMap<>();

        GraphFragment rootFragment = fragmentBuilder.buildFragment(root, graph);
        fragmentMap.put(root, rootFragment);
        graph.addEntryNode(rootFragment.entryNode());
        recordData(root, rootFragment.internalEdges(), traceIds, dataRows);

        traverse(root, rootFragment, graph, fragmentMap, traceIds, dataRows);

        return graph;
    }

    // -------------------------------------------------------------------------
    // Traversal
    // -------------------------------------------------------------------------

    private void traverse(
            ClusteredAbstractSpan parentSpan,
            GraphFragment parentFragment,
            Graph graph,
            Map<ClusteredAbstractSpan, GraphFragment> fragmentMap,
            List<String> traceIds,
            Map<EdgeSpan, List<Integer>> dataRows
    ) {
        for (ClusteredAbstractSpan childSpan : parentSpan.getChildren()) {
            GraphFragment childFragment = fragmentBuilder.buildFragment(childSpan, graph);
            fragmentMap.put(childSpan, childFragment);
            recordData(childSpan, childFragment.internalEdges(), traceIds, dataRows);

            List<CallEdge> edges = fragmentBuilder.connectFragments(
                    parentFragment, parentSpan,
                    childFragment,  childSpan,
                    graph
            );
            recordData(childSpan, edges, traceIds, dataRows);

            relateCalls(parentSpan, parentFragment, childSpan, childFragment, edges, fragmentMap, graph, traceIds, dataRows);

            relateInternalCalls(childSpan, childFragment, edges, graph, traceIds, dataRows);

            traverse(childSpan, childFragment, graph, fragmentMap, traceIds, dataRows);
        }
    }

    // -------------------------------------------------------------------------
    // Call occurrence relationships; inference belongs to Graph.finalizeGraph.
    // -------------------------------------------------------------------------

    private void relateInternalCalls(
            ClusteredAbstractSpan childSpan,
            GraphFragment childFragment,
            List<CallEdge> connectingEdges,
            Graph graph,
            List<String> traceIds,
            Map<EdgeSpan, List<Integer>> dataRows
    ) {
        for (CallEdge connectingEdge : connectingEdges) {
            // A shared boundary can make the connector an internal edge already.
            if (childFragment.internalEdges().contains(connectingEdge)) {
                continue;
            }

            for (CallEdge internalEdge : childFragment.internalEdges()) {
                if (connectingEdge.getTo() == internalEdge.getFrom()) {
                    relate(connectingEdge, childSpan, internalEdge, childSpan, graph, traceIds, dataRows);
                }
            }
        }
    }

    private void relateCalls(
            ClusteredAbstractSpan parentSpan,
            GraphFragment parentFragment,
            ClusteredAbstractSpan childSpan,
            GraphFragment childFragment,
            List<CallEdge> edges,
            Map<ClusteredAbstractSpan, GraphFragment> fragmentMap,
            Graph graph,
            List<String> traceIds,
            Map<EdgeSpan, List<Integer>> dataRows
    ) {

        for (Edge edge : parentFragment.exitNode().getIncoming()) {
            if(!(edge instanceof CallEdge callEdge)){
                continue;
            }

            for (CallEdge targetEdge : edges) {
                relate(callEdge, parentSpan, targetEdge, childSpan, graph, traceIds, dataRows);
            }
        }

        List<ClusteredAbstractSpan> children = parentSpan.getChildren();
        if (children.size() < 2) return;

        for (ClusteredAbstractSpan sourceSpan : children) {
            GraphFragment sourceFragment = fragmentMap.get(sourceSpan);
            if (sourceFragment == null) continue;
            if (sourceFragment == childFragment) continue;

            // Collect all outgoing CallEdges from the source fragment's exit node
            List<Edge> sourceEdges = parentFragment.exitNode().findOutgoingTo(sourceFragment.entryNode());
            if (sourceEdges.isEmpty()) continue;

            for (Edge edge : sourceEdges) {
                if (!(edge instanceof CallEdge callEdge)) {
                    continue;
                }

                for (CallEdge targetEdge : edges) {
                    relate(callEdge, sourceSpan, targetEdge, childSpan, graph, traceIds, dataRows);
                }
            }
        }
    }

    private void recordData(ClusteredAbstractSpan span, List<CallEdge> edges, List<String> traceIds,
                            Map<EdgeSpan, List<Integer>> dataRows) {
        if (span.getSpanData().size() != traceIds.size()) {
            throw new IllegalArgumentException("Clustered spans must retain one data slot per execution.");
        }

        for (CallEdge edge : edges) {
            EdgeSpan binding = new EdgeSpan(edge, span);
            if (dataRows.containsKey(binding)) {
                continue;
            }

            List<Integer> rows = new ArrayList<>();
            for (int index = 0; index < traceIds.size(); index++) {
                SpanData data = span.getSpanData().get(index);
                rows.add(edge.addData(traceIds.get(index), data == null ? null : data.getInputData(),
                        data == null ? null : data.getOutputData()));
            }
            dataRows.put(binding, rows);
        }
    }

    private void relate(CallEdge source, ClusteredAbstractSpan sourceSpan, CallEdge target,
                        ClusteredAbstractSpan targetSpan, Graph graph,
                        List<String> traceIds, Map<EdgeSpan, List<Integer>> dataRows) {
        List<Integer> sources = dataRows.get(new EdgeSpan(source, sourceSpan));
        List<Integer> targets = dataRows.get(new EdgeSpan(target, targetSpan));
        // An incoming graph edge can belong to another span at the shared service node.
        if (sources == null || targets == null) {
            return;
        }

        List<CallRelation> comparableCalls = new ArrayList<>();
        for (int index = 0; index < sources.size(); index++) {
            if (source == target && sources.get(index).equals(targets.get(index))) {
                continue;
            }
            comparableCalls.add(new CallRelation(traceIds.get(index), source, sources.get(index), target, targets.get(index)));
        }
        graph.addCallRelationGroup(comparableCalls);
    }

    private record EdgeSpan(CallEdge edge, ClusteredAbstractSpan span) {}
}
