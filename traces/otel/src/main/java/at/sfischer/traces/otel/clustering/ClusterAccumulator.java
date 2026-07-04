package at.sfischer.traces.otel.clustering;

import at.sfischer.traces.otel.TraceNode;

public interface ClusterAccumulator<T extends TraceNode<T>, S> {

    S create(T firstTrace);

    S update(S currentState, T trace);
}
