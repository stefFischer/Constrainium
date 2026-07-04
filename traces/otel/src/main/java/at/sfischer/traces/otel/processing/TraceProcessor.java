package at.sfischer.traces.otel.processing;

import at.sfischer.traces.otel.TraceNode;

import java.util.List;

public interface TraceProcessor<I extends TraceNode<I>, O extends TraceNode<O>> {

    List<O> process(List<I> input);

    default <NEXT extends TraceNode<NEXT>> TraceProcessor<I, NEXT> pipe(TraceProcessor<O, NEXT> next) {
        return input -> {
            List<O> intermediate = process(input);
            return next.process(intermediate);
        };
    }
}
