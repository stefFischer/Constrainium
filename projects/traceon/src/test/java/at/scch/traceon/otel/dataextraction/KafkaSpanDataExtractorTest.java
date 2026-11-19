package at.scch.traceon.otel.dataextraction;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.dataextraction.SpanData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class KafkaSpanDataExtractorTest {

    private static final String TRACE_ID = "3158d5fa3eca9c6a76dc00f2905f97b0";
    private static final String PUBLISHER_ID = "65c1aa29811f2d11";

    private static final String ORDER_RECORD = """
            {
              "source_kind": "kafka_record",
              "source": "order.created",
              "trace_id": "3158d5fa3eca9c6a76dc00f2905f97b0",
              "span_id": "65c1aa29811f2d11",
              "kafka": {"topic": "order.created", "partition": 0, "offset": 325},
              "payload": {"orderId": "order-1"}
            }
            """;

    @ParameterizedTest
    @ValueSource(strings = {"X-B3-SpanId", "x-b3-spanid"})
    void matchesInvoiceConsumerThroughPropagatedPublisherContext(String spanIdAttribute) {
        Span consumer = createInvoiceConsumer(TRACE_ID);
        consumer.getAttributes().put(spanIdAttribute, PUBLISHER_ID);
        consumer.getAttributes().put("X-B3-TraceId", TRACE_ID);

        SpanData data = extract(consumer, ORDER_RECORD);

        assertEquals("order-1", getOrderId(data.getInputData()));
        assertNull(data.getOutputData());
        assertEquals("consumer", consumer.getKind());
    }

    @Test
    void keepsExistingDirectMatchWhenConsumerAlsoHasPropagatedContext() {
        Span consumer = new Span(
                "kafka.consume", PUBLISHER_ID, TRACE_ID, "084fc5502a14b96d",
                "consumer", "order-service", 0, 1
        );
        consumer.getAttributes().put("messaging.destination", "checkout.orders");
        consumer.getAttributes().put("X-B3-SpanId", "084fc5502a14b96d");
        consumer.getAttributes().put("X-B3-TraceId", TRACE_ID);

        String checkoutRecord = """
                {
                  "source_kind": "kafka_record",
                  "source": "checkout.orders",
                  "span_id": "084fc5502a14b96d",
                  "payload": {"orderId": "different-message"}
                }
                """;
        SpanData data = extract(consumer, ORDER_RECORD, checkoutRecord);

        assertEquals("order-1", getOrderId(data.getInputData()));
        assertNull(data.getOutputData());
    }

    @Test
    void validatesTopicAndPropagatedTraceWithoutUsingTimestamps() {
        Span consumer = createInvoiceConsumer("different-local-trace");
        consumer.getAttributes().put("X-B3-SpanId", PUBLISHER_ID);
        consumer.getAttributes().put("x-b3-traceid", TRACE_ID);

        String wrongTopic = ORDER_RECORD.replace("order.created", "another.topic");
        String wrongTrace = ORDER_RECORD.replace(TRACE_ID, "another-trace");
        String wrongPublisher = ORDER_RECORD.replace(PUBLISHER_ID, "another-publisher");
        String wrongSourceKind = ORDER_RECORD.replace("kafka_record", "postgres_row");

        SpanData data = extract(
                consumer, wrongTopic, wrongTrace, wrongPublisher, wrongSourceKind, ORDER_RECORD
        );

        assertEquals("order-1", getOrderId(data.getInputData()));
        assertNull(extract(consumer, wrongTopic, wrongTrace, wrongPublisher, wrongSourceKind).getInputData());
    }

    @Test
    void doesNotApplyConsumerFallbackToProducersOrWithoutATopic() {
        Span producer = new Span(
                "publish", "other-span", TRACE_ID, null,
                "producer", "test", 0, 1
        );
        producer.getAttributes().put("X-B3-SpanId", PUBLISHER_ID);
        producer.getAttributes().put("messaging.destination", "order.created");
        assertNull(extract(producer, ORDER_RECORD).getInputData());

        Span consumer = createInvoiceConsumer(TRACE_ID);
        consumer.getAttributes().put("X-B3-SpanId", PUBLISHER_ID);
        consumer.removeAttribute("messaging.destination.name");
        assertNull(extract(consumer, ORDER_RECORD).getInputData());
        consumer.getAttributes().put("messaging.destination", "order.created");
        assertEquals("order-1", getOrderId(extract(consumer, ORDER_RECORD).getInputData()));
    }

    private Span createInvoiceConsumer(String traceId) {
        Span consumer = new Span(
                "order.created process", "0a3e7fb5cc726782", traceId, PUBLISHER_ID,
                "consumer", "invoice-service", 0, 1
        );
        consumer.getAttributes().put("messaging.destination.name", "order.created");
        return consumer;
    }

    private String getOrderId(DataObject data) {
        assertNotNull(data);
        DataObject payload = (DataObject) data.getDataValue("payload").getValue();
        return (String) payload.getDataValue("orderId").getValue();
    }

    @Test
    void correlatesIdsAndKeepsOnlyBusinessPayload() throws Exception {
        SpanData data = extract("producer", """
                {"source_kind":"kafka_record","span_id":"span","trace_id":"trace",
                 "source":"checkout.orders","kafka":{"offset":320},"payload":{"cartId":"cart-1"}}
                """,
                "{source_kind:'kafka_record',span_id:'other',trace_id:'trace',payload:{wrong:true}}",
                "{source_kind:'kafka_record',span_id:'span',trace_id:'other',payload:{wrong:true}}",
                "{source_kind:'postgres_row',span_id:'span',trace_id:'trace',payload:{wrong:true}}");
        assertNull(data.getOutputData());
        ObjectMapper json = new ObjectMapper();
        assertEquals(json.readTree("{\"payload\":{\"cartId\":\"cart-1\"}}"), json.readTree(data.getInputData().toJson()));
    }

    @Test
    void preservesInputConventionForRecordsPublishedInsideConsumerAndSupportsMultipleRecords() {
        SpanData data = extract("consumer",
                "{source_kind:'kafka_record',span_id:'span',payload:{orderId:'one'}}",
                "{source_kind:'kafka_record',span_id:'span',payload:{orderId:'two'}}");
        assertNull(data.getOutputData());
        DataObject[] results = (DataObject[]) data.getInputData().getDataValue("results").getValue();
        assertEquals(2, results.length);
        assertEquals("one", ((DataObject) results[0].getDataValue("payload").getValue()).getDataValue("orderId").getValue());
        assertEquals("two", ((DataObject) results[1].getDataValue("payload").getValue()).getDataValue("orderId").getValue());
    }

    @Test
    void leavesUnmatchedOrMissingPayloadsEmpty() {
        SpanData data = extract("producer",
                "{source_kind:'kafka_record',span_id:'other',payload:{orderId:'one'}}",
                "{source_kind:'kafka_record',payload:{orderId:'two'}}",
                "{source_kind:'kafka_record',span_id:'span'}");
        assertNull(data.getInputData());
        assertNull(data.getOutputData());
    }

    private SpanData extract(String kind, String... records) {
        Span span = new Span("kafka", "span", "trace", null, kind, "test", 0, 100);
        return extract(span, records);
    }

    private SpanData extract(Span span, String... records) {
        SimpleDataCollection collection = new SimpleDataCollection();
        for (String record : records) {
            collection.addDataEntry(DataObject.parseData(record));
        }

        return new KafkaSpanDataExtractor(collection).extractData(span);
    }
}
