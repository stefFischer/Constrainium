package at.sfischer.traces.otel;

public interface TraceNodeVisitor<T extends TraceNode<T>> {

    /**
     * Visits a span.
     *
     * @param tranceNode Span that is visited.
     * @return true: if the children of span should also be visited. false: otherwise.
     */
    boolean visit(T tranceNode);
}
