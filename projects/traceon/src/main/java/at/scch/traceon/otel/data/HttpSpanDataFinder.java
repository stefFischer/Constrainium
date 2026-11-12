package at.scch.traceon.otel.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.traces.otel.Span;

import java.util.List;

public class HttpSpanDataFinder extends AbstractHttpSpanDataFinder {

    public HttpSpanDataFinder(DataCollection<DataObject> data) {
        super(data);
    }

    @Override
    protected Constraint createConstraint(Span span) {
        if (isServer(span)) {
            return new Constraint(serverRequestConstraint(span));
        }

        return new Constraint(clientRequestConstraint(span));
    }

    @Override
    public DataCollection<DataObject> findData(Span span) {
        if (isServer(span)) {
            return closestTo(super.findData(span), span.getStart());
        }

        DataCollection<DataObject> direct = super.findData(span);
        if (direct.size() > 0) {
            return closestTo(direct, span.getStart());
        }

        DataCollection<DataObject> fallback = captures.filter(new Constraint(clientRequestFallbackConstraint(span)));
        return closestTo(fallback, span.getStart());
    }

    private Node serverRequestConstraint(Span span) {
        return andAll(List.of(
                httpSource(),
                equal(DIRECTION, REQUEST),
                endpointMatches(span),
                traceMatches(span),
                timeWindow(span)
        ));
    }

    private Node clientRequestConstraint(Span span) {
        return andAll(List.of(
                httpSource(),
                equal(DIRECTION, REQUEST),
                headerEquals(B3_SPAN_ID, span.getSpanId()),
                traceMatches(span)
        ));
    }

    private Node clientRequestFallbackConstraint(Span span) {
        String parentSpanId = span.getParentSpanId();
        if (parentSpanId == null) {
            return BooleanLiteral.FALSE;
        }

        return andAll(List.of(
                httpSource(),
                equal(DIRECTION, REQUEST),
                headerEquals(B3_SPAN_ID, parentSpanId),
                endpointMatches(span),
                traceMatches(span),
                timeWindow(span)
        ));
    }
}
