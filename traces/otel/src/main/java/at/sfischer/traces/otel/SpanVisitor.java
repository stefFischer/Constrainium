package at.sfischer.traces.otel;

public interface SpanVisitor {

    /**
     * Visits a span.
     *
     * @param span Span that is visited.
     * @return true: if the children of span should also be visited. false: otherwise.
     */
    boolean visitSpan(Span span);
}
