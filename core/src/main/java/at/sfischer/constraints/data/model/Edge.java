package at.sfischer.constraints.data.model;

public abstract class Edge {
    private final GraphNode from;
    private final GraphNode to;

    public Edge(GraphNode from, GraphNode to) {
        this.from = from;
        this.to = to;

        from.addOutgoing(this);
        to.addIncoming(this);
    }

    public GraphNode getFrom() { return from; }

    public GraphNode getTo() { return to; }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
