package at.sfischer.traces.otel.graph;

import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.model.CallEdge;
import at.sfischer.constraints.data.model.Edge;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CallGraphBuilder {

    private final SpanFragmentBuilder fragmentBuilder;

    public CallGraphBuilder(SpanFragmentBuilder fragmentBuilder) {
        this.fragmentBuilder = fragmentBuilder;
    }

    public Graph build(ClusteredAbstractSpan root) {
        String traceId = UUID.randomUUID().toString();
        Graph graph = new Graph();
        Map<ClusteredAbstractSpan, GraphFragment> fragmentMap = new IdentityHashMap<>();

        GraphFragment rootFragment = fragmentBuilder.buildFragment(root, graph);
        fragmentMap.put(root, rootFragment);
        graph.addEntryNode(rootFragment.entryNode());

        traverse(root, rootFragment, graph, fragmentMap, traceId);

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
            String traceId
    ) {
        for (ClusteredAbstractSpan childSpan : parentSpan.getChildren()) {
            GraphFragment childFragment = fragmentBuilder.buildFragment(childSpan, graph);
            fragmentMap.put(childSpan, childFragment);

            List<CallEdge> edges = fragmentBuilder.connectFragments(
                    parentFragment, parentSpan,
                    childFragment,  childSpan,
                    graph
            );

            inferDataFlows(parentSpan, parentFragment, childSpan, childFragment, edges, fragmentMap, traceId);

            traverse(childSpan, childFragment, graph, fragmentMap, traceId);
        }
    }

    // -------------------------------------------------------------------------
    // Data flow inference
    // -------------------------------------------------------------------------

    private void inferDataFlows(
            ClusteredAbstractSpan parentSpan,
            GraphFragment parentFragment,
            ClusteredAbstractSpan childSpan,
            GraphFragment childFragment,
            List<CallEdge> edges,
            Map<ClusteredAbstractSpan, GraphFragment> fragmentMap,
            String traceId
    ) {

        InOutputDataCollection parentData = parentSpan.getSpanDataCollection();
        InOutputDataCollection childData = childSpan.getSpanDataCollection();
        for (Edge edge : parentFragment.exitNode().getIncoming()) {
            if(!(edge instanceof CallEdge callEdge)){
                continue;
            }

            for (CallEdge targetEdge : edges) {
                callEdge.inferDataFlows(traceId, parentData, targetEdge, childData);
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

            InOutputDataCollection sourceData = sourceSpan.getSpanDataCollection();
            for (Edge edge : sourceEdges) {
                if (!(edge instanceof CallEdge callEdge)) {
                    continue;
                }

                for (CallEdge targetEdge : edges) {
                    callEdge.inferDataFlows(traceId, sourceData, targetEdge, childData);
                }
            }
        }
    }
}
