package at.sfischer.traces.otel.collector.buffer;

import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.collector.TraceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TraceBuffer implements TraceListener<Span> {

    private final Logger LOGGER = LoggerFactory.getLogger(TraceBuffer.class);

    private final Map<String, TraceState> traces = new LinkedHashMap<>();
    private final List<TraceListener<Span>> listeners = new ArrayList<>();

    private final int maxInactiveCycles;

    public TraceBuffer(int maxInactiveCycles) {
        this.maxInactiveCycles = maxInactiveCycles;
    }

    public void addTraceListener(TraceListener<Span> listener) {
        listeners.add(listener);
    }

    public void removeTraceListener(TraceListener<Span> listener) {
        listeners.remove(listener);
    }

    @Override
    public void spansCollected(List<Span> spans) {
        Set<String> touchedTraceIds = new HashSet<>();
        for (Span span : spans) {
            String traceId = span.getTraceId();
            TraceState state = traces.computeIfAbsent(
                    traceId,
                    id -> new TraceState(traceId)
            );

            addSpanIncrementally(state, span);
            touchedTraceIds.add(traceId);
        }

        for (Map.Entry<String, TraceState> entry : traces.entrySet()) {
            String traceId = entry.getKey();
            TraceState state = entry.getValue();
            if (!touchedTraceIds.contains(traceId)) {
                state.notTouched();
            }
        }

        Set<String> emitted = new HashSet<>();
        traces.forEach((traceId, state) -> {
            if(state.getRoots().isEmpty()){
                return;
            }

            boolean inactiveTooLong = state.getInactiveCycles() >= maxInactiveCycles;
            boolean noOrphans = state.getWaitingChildren().isEmpty();
            if (inactiveTooLong && noOrphans) {
                emit(state.getRoots());
                emitted.add(traceId);
            }
        });
        emitted.forEach(traces::remove);
    }

    public void flush() {
        for (TraceState state : traces.values()) {
            if(!state.getWaitingChildren().isEmpty()){
                LOGGER.error("Orphan spans ({}) remaining for traceid: {}", state.getWaitingChildren().size(), state.getTraceId());
            }
            if(state.getRoots().isEmpty()){
                continue;
            }

            emit(state.getRoots());
        }

        traces.clear();
    }

    private void emit(List<Span> spans) {
        for (TraceListener<Span> listener : listeners) {
            listener.spansCollected(spans);
        }
    }

    private static void addSpanIncrementally(TraceState state, Span span) {
        if (state.containsSpan(span)) {
            return;
        }

        state.addSpan(span);
        String parentId = span.getParentSpanId();

        if (parentId == null) {
            state.getRoots().add(span);
        } else {
            Span parent = state.getSpan(parentId);
            if (parent != null) {
                parent.addChild(span);
            } else {
                state.getWaitingChildren()
                        .computeIfAbsent(parentId, k -> new LinkedList<>())
                        .add(span);
            }
        }

        List<Span> waiting = state.getWaitingChildren().remove(span.getSpanId());

        if (waiting != null) {
            for (Span child : waiting) {
                span.addChild(child);
            }
        }
    }
}
