package at.sfischer.traces.otel.clustering;

import at.sfischer.traces.otel.abstraction.AbstractSpan;

public class ClusteredAbstractSpanAccumulator implements ClusterAccumulator<AbstractSpan, ClusteredAbstractSpan>{

    @Override
    public ClusteredAbstractSpan create(AbstractSpan firstTrace) {
        return new ClusteredAbstractSpan(firstTrace);
    }

    @Override
    public ClusteredAbstractSpan update(ClusteredAbstractSpan currentState, AbstractSpan trace) {
        currentState.addSpan(trace);
        return currentState;
    }
}
