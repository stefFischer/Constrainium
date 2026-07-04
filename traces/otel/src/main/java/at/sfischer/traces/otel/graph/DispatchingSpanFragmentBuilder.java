package at.sfischer.traces.otel.graph;

import at.sfischer.constraints.data.model.CallEdge;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;

import java.util.List;

public class DispatchingSpanFragmentBuilder implements SpanFragmentBuilder {

    private final List<MatchingSpanFragmentBuilder> builders;

    public DispatchingSpanFragmentBuilder(MatchingSpanFragmentBuilder... builders) {
        this.builders = List.of(builders);
    }

    @Override
    public GraphFragment buildFragment(ClusteredAbstractSpan span, Graph graph) {
        return builders.stream()
                .filter(b -> b.match().matches(span).isMatch())
                .findFirst()
                .map(b -> b.buildFragment(span, graph))
                .orElse(null);
    }

    @Override
    public List<CallEdge> connectFragments(
            GraphFragment parent, ClusteredAbstractSpan parentSpan,
            GraphFragment child, ClusteredAbstractSpan childSpan,
            Graph graph
    ) {
        return builders.stream()
                .filter(b -> b.match().matches(childSpan).isMatch())
                .findFirst()
                .map(b -> b.connectFragments(parent, parentSpan, child, childSpan, graph))
                .orElse(List.of());
    }
}