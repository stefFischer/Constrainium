package at.sfischer.traces.otel.clustering;

import at.sfischer.traces.otel.TraceNode;

public class TraceCluster<T extends TraceNode<T>, S> {

    private final T representative;

    private S state;

    public TraceCluster(T representative, S initialState) {
        this.representative = representative;
        this.state = initialState;
    }

    public T getRepresentative() {
        return representative;
    }

    public S getState() {
        return state;
    }

    void updateState(S state) {
        this.state = state;
    }
}
