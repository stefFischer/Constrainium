package at.sfischer.traces.otel.clustering;

import at.sfischer.traces.otel.TraceNode;
import at.sfischer.traces.otel.differ.SpanComparator;
import at.sfischer.traces.otel.differ.TraceDiffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TraceClusterer<T extends TraceNode<T>, S> {

    private final SpanComparator<T> comparator;
    private final ClusterAccumulator<T, S> accumulator;

    private final List<TraceCluster<T, S>> clusters = new ArrayList<>();

    public TraceClusterer(
            SpanComparator<T> comparator,
            ClusterAccumulator<T, S> accumulator) {

        this.comparator = comparator;
        this.accumulator = accumulator;
    }

    public TraceCluster<T, S> add(T trace) {
        for (TraceCluster<T, S> cluster : clusters) {
            if (isSameCluster(cluster.getRepresentative(), trace)) {
                S updated = accumulator.update(cluster.getState(), trace);
                cluster.updateState(updated);
                return cluster;
            }
        }

        S initialState = accumulator.create(trace);
        TraceCluster<T, S> cluster = new TraceCluster<>(trace, initialState);
        clusters.add(cluster);
        return cluster;
    }

    public List<TraceCluster<T, S>> getClusters() {
        return Collections.unmodifiableList(clusters);
    }

    private boolean isSameCluster(T representative, T trace) {
        return TraceDiffer.diff(
                representative,
                trace,
                comparator).isEmpty();
    }
}
