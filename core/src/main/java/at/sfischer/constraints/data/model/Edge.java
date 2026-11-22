package at.sfischer.constraints.data.model;

public abstract class Edge {
    private final GraphNode from;
    private final GraphNode to;
    private boolean frozen;

    public Edge(GraphNode from, GraphNode to) {
        // Validate both endpoints before registering either side of the edge.
        from.checkMutable();
        to.checkMutable();
        this.from = from;
        this.to = to;

        from.addOutgoing(this);
        to.addIncoming(this);
    }

    public GraphNode getFrom() { return from; }

    public GraphNode getTo() { return to; }

    public boolean isFrozen() {
        return frozen;
    }

    protected final void checkMutable() {
        if (frozen) {
            throw new IllegalStateException("The graph edge is finalized.");
        }
    }

    void freeze() {
        frozen = true;
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
