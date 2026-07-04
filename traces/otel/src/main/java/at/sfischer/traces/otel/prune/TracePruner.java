package at.sfischer.traces.otel.prune;

import at.sfischer.traces.otel.TraceNode;
import at.sfischer.traces.otel.processing.TraceProcessor;

import java.util.List;

public interface TracePruner <T extends TraceNode<T>> extends TraceProcessor<T, T> {
    void prune(T root);

    @Override
    default List<T> process(List<T> input) {
        input.forEach(this::prune);
        return input;
    }
}
