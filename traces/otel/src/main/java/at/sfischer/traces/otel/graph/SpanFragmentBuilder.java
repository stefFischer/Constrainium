package at.sfischer.traces.otel.graph;

import at.sfischer.constraints.data.model.CallEdge;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;

import java.util.List;

public interface SpanFragmentBuilder {

    /**
     * Builds a graph fragment for a single span and registers its
     * internal nodes/edges into the provided graph.
     *
     * @param span  the span to build a fragment for
     * @param graph the graph to register nodes into
     * @return a fragment exposing the entry and exit node
     */
    GraphFragment buildFragment(ClusteredAbstractSpan span, Graph graph);

    /**
     * Connects the exit node of a parent fragment to the entry node of a
     * child fragment, returning the edge(s) created. The implementation
     * owns this decision since it knows the node structure and call semantics.
     *
     * @param parent     the fragment produced for the parent span
     * @param parentSpan the parent span (for schema/data derivation)
     * @param child      the fragment produced for the child span
     * @param childSpan  the child span (for kind, schema/data derivation)
     * @param graph      the graph to register any new nodes into
     * @return the edges connecting the two fragments (may be empty)
     */
    List<CallEdge> connectFragments(
            GraphFragment parent, ClusteredAbstractSpan parentSpan,
            GraphFragment child,  ClusteredAbstractSpan childSpan,
            Graph graph
    );
}