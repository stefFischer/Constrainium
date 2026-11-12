package at.scch.traceon.otel.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.traces.otel.Span;

import java.util.ArrayList;
import java.util.List;

public class HttpResponseSpanDataFinder extends AbstractHttpSpanDataFinder {

    public HttpResponseSpanDataFinder(DataCollection<DataObject> data) {
        super(data);
    }

    @Override
    protected Constraint createConstraint(Span span) {
        if (isServer(span)) {
            return new Constraint(serverResponseConstraint(span));
        }

        return new Constraint(clientResponseConstraint(span));
    }

    @Override
    public DataCollection<DataObject> findData(Span span) {
        return closestTo(super.findData(span), span.getEnd());
    }

    private Node serverResponseConstraint(Span span) {
        return andAll(List.of(
                httpSource(),
                equal(DIRECTION, RESPONSE),
                headerEquals(B3_SPAN_ID, span.getSpanId()),
                traceMatches(span),
                statusMatches(span),
                timeWindow(span)
        ));
    }

    private Node clientResponseConstraint(Span span) {
        List<Node> spanIdMatches = new ArrayList<>();
        for (String serverSpanId : serverChildSpanIds(span)) {
            spanIdMatches.add(headerEquals(B3_SPAN_ID, serverSpanId));
        }

        Node spanIdMatch = spanIdMatches.isEmpty() ? BooleanLiteral.TRUE : orAll(spanIdMatches);

        return andAll(List.of(
                httpSource(),
                equal(DIRECTION, RESPONSE),
                spanIdMatch,
                traceMatches(span),
                statusMatches(span),
                timeWindow(span)
        ));
    }

    private static List<String> serverChildSpanIds(Span span) {
        List<String> spanIds = new ArrayList<>();
        for (Span child : span.getChildren()) {
            if ("server".equalsIgnoreCase(child.getKind()) && child.getSpanId() != null) {
                spanIds.add(child.getSpanId());
            }
        }
        return spanIds;
    }
}
