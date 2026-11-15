package at.scch.traceon.otel.dataextraction;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.dataextraction.SpanData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaSpanDataExtractorTest {
    @Test
    void correlatesIdsAndKeepsOnlyBusinessPayload() throws Exception {
        SpanData data = extract("producer", """
                {"source_kind":"kafka_record","span_id":"span","trace_id":"trace",
                 "source":"checkout.orders","kafka":{"offset":320},"payload":{"cartId":"cart-1"}}
                """,
                "{source_kind:'kafka_record',span_id:'other',trace_id:'trace',payload:{wrong:true}}",
                "{source_kind:'kafka_record',span_id:'span',trace_id:'other',payload:{wrong:true}}",
                "{source_kind:'postgres_row',span_id:'span',trace_id:'trace',payload:{wrong:true}}");
        assertNull(data.getInputData());
        ObjectMapper json = new ObjectMapper();
        assertEquals(json.readTree("{\"payload\":{\"cartId\":\"cart-1\"}}"), json.readTree(data.getOutputData().toJson()));
    }

    @Test
    void treatsRecordsPublishedInsideConsumerAsOutputAndSupportsMultipleRecords() {
        SpanData data = extract("consumer",
                "{source_kind:'kafka_record',span_id:'span',payload:{orderId:'one'}}",
                "{source_kind:'kafka_record',span_id:'span',payload:{orderId:'two'}}");
        assertNull(data.getInputData());
        DataObject[] results = (DataObject[]) data.getOutputData().getDataValue("results").getValue();
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
        SimpleDataCollection collection = new SimpleDataCollection();
        for (String record : records) collection.addDataEntry(DataObject.parseData(record));
        Span span = new Span("kafka", "span", "trace", null, kind, "test", 0, 100);
        return new KafkaSpanDataExtractor(collection).extractData(span);
    }
}
