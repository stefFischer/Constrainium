package at.scch.traceon.otel.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.StringLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.FirstValue;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.abstraction.SpanDataFinder;

/** Finds records published in the given span's context. */
public class KafkaSpanDataFinder extends SpanDataFinder<DataObject> {
    public KafkaSpanDataFinder(DataCollection<DataObject> records) {
        super(records);
    }

    @Override
    protected Constraint createConstraint(Span span) {
        if (span.getSpanId() == null) return new Constraint(BooleanLiteral.FALSE);
        // Missing trace IDs are allowed, but supplied IDs must agree with the span.
        StringLiteral traceId = new StringLiteral(span.getTraceId() == null ? "" : span.getTraceId());
        return new Constraint(and(
                equal("source_kind", "kafka_record"),
                and(
                        equal("span_id", span.getSpanId()),
                        new StringEquals(new FirstValue(new Variable("trace_id"), traceId), traceId)
                )
        ));
    }
}
