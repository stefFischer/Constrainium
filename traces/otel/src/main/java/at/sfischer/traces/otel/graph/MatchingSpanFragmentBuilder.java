package at.sfischer.traces.otel.graph;

import at.sfischer.constraints.data.model.CallEdge;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;
import at.sfischer.traces.otel.matching.TraceNodeMatch;

import java.util.List;

public interface MatchingSpanFragmentBuilder {

    TraceNodeMatch<ClusteredAbstractSpan> match();

    GraphFragment buildFragment(ClusteredAbstractSpan span, Graph graph);

    List<CallEdge> connectFragments(
            GraphFragment parent, ClusteredAbstractSpan parentSpan,
            GraphFragment child, ClusteredAbstractSpan childSpan,
            Graph graph
    );
}
