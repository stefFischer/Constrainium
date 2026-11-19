package at.scch.traceon.otel.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.StringLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.FirstValue;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.Attribute;
import at.sfischer.traces.otel.abstraction.SpanDataFinder;

/** Finds Kafka records using the span's own context or a consumer's propagated context. */
public class KafkaSpanDataFinder extends SpanDataFinder<DataObject> {

    private final DataCollection<DataObject> records;

    public KafkaSpanDataFinder(DataCollection<DataObject> records) {
        super(records);
        this.records = records;
    }

    @Override
    public DataCollection<DataObject> findData(Span span) {
        DataCollection<DataObject> directMatches = super.findData(span);
        if (directMatches.size() > 0 || !"consumer".equalsIgnoreCase(span.getKind())) {
            return directMatches;
        }

        return records.filter(createPropagatedContextConstraint(span));
    }

    @Override
    protected Constraint createConstraint(Span span) {
        if (span.getSpanId() == null) {
            return new Constraint(BooleanLiteral.FALSE);
        }

        return new Constraint(recordContextMatches(span.getSpanId(), span.getTraceId()));
    }

    private Constraint createPropagatedContextConstraint(Span span) {
        String publisherSpanId = getPropagatedAttribute(span, "x-b3-spanid");
        String publisherTraceId = getPropagatedAttribute(span, "x-b3-traceid");
        String topic = span.getAttribute("messaging.destination.name");
        if (topic == null) {
            topic = span.getAttribute("messaging.destination");
        }

        if (publisherSpanId == null || topic == null) {
            return new Constraint(BooleanLiteral.FALSE);
        }

        if (publisherTraceId == null) {
            publisherTraceId = span.getTraceId();
        }

        Node recordTopic = new FirstValue(new Variable("kafka.topic"), new Variable("source"));
        Node topicMatches = new StringEquals(recordTopic, new StringLiteral(topic));
        Node publisherMatches = recordContextMatches(publisherSpanId, publisherTraceId);

        return new Constraint(and(publisherMatches, topicMatches));
    }

    private Node recordContextMatches(String spanId, String traceId) {
        // Missing trace IDs are allowed, but supplied IDs must agree with the span.
        StringLiteral expectedTraceId = new StringLiteral(traceId == null ? "" : traceId);
        Node recordTraceId = new FirstValue(new Variable("trace_id"), expectedTraceId);
        Node traceMatches = new StringEquals(recordTraceId, expectedTraceId);

        return and(
                equal("source_kind", "kafka_record"),
                and(
                        equal("span_id", spanId),
                        traceMatches
                )
        );
    }

    private String getPropagatedAttribute(Span span, String name) {
        for (Attribute<?> attribute : span.getAttributes().getAttributes()) {
            if (name.equalsIgnoreCase(attribute.getKey()) && attribute.getValue() instanceof String value) {
                return value.isBlank() ? null : value;
            }
        }

        return null;
    }
}
