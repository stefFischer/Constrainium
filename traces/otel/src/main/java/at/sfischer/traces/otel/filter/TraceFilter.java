package at.sfischer.traces.otel.filter;

import at.sfischer.traces.otel.TraceNode;
import at.sfischer.traces.otel.processing.TraceProcessor;

import java.util.List;

public interface TraceFilter<T extends TraceNode<T>> extends TraceProcessor<T, T> {

    default List<T> filter(List<T> input){
        return input.stream().filter(this::include).toList();
    }

    boolean include(T node);

    @Override
    default List<T> process(List<T> input) {
        return filter(input);
    }
}
