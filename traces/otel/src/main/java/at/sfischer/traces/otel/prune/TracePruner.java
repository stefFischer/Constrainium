package at.sfischer.traces.otel.prune;

import at.sfischer.traces.otel.TraceNode;

public interface TracePruner <T extends TraceNode<T>> {
    void prune(T root);
}
