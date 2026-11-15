package at.scch.traceon.otel.dataextraction;

import at.scch.traceon.otel.MongoPayloadDecoder;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.dataextraction.SpanData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.RawBsonDocument;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class MongoSpanDataExtractorTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void matchesRequestAndResponseByTheirRespectiveIdsAndOperation() throws Exception {
        // This capture format uses span request ID + 1 as the wire request ID.
        SpanData data = extract(
                capture("mongo.request_id", "107", "find", encoded("{\"filter\":{\"sku\":\"STRW-001\"}}")),
                capture("mongo.response_to", "107", "find", encoded("{\"sku\":\"STRW-001\",\"stock\":219}")),
                capture("mongo.request_id", "106", "find", encoded("{\"wrong\":true}")),
                capture("mongo.response_to", "108", "find", encoded("{\"wrong\":true}")),
                capture("mongo.request_id", "107", "update", encoded("{\"wrong\":true}")),
                capture("mongo.response_to", "107", "update", encoded("{\"wrong\":true}")));

        assertEquals(json.readTree("{\"payload\":{\"filter\":{\"sku\":\"STRW-001\"}}}"), tree(data.getInputData()));
        assertEquals(json.readTree("{\"payload\":{\"sku\":\"STRW-001\",\"stock\":219}}"), tree(data.getOutputData()));
    }

    @Test
    void decodesBsonWireMessagesThroughTheRealExtractor() throws Exception {
        SpanData data = extract(
                capture("mongo.request_id", "107", "find", opMsg(107, 0,
                        "{\"find\":\"products\",\"filter\":{\"sku\":\"STRW-001\"},\"$db\":\"product_svc\"}")),
                capture("mongo.response_to", "107", "find", opMsg(900, 107,
                        "{\"cursor\":{\"firstBatch\":[{\"sku\":\"STRW-001\",\"stock\":219}]},\"ok\":1}")));

        JsonNode request = tree(data.getInputData()).path("payload").get(0);
        JsonNode response = tree(data.getOutputData()).path("payload").get(0);
        assertEquals("products", request.path("find").asText());
        assertEquals("STRW-001", request.path("filter").path("sku").asText());
        assertEquals("product_svc", request.path("$db").asText());
        assertEquals(219, response.path("cursor").path("firstBatch").get(0).path("stock").asInt());
    }

    @Test
    void leavesUnmatchedAndBodylessDirectionsNull() {
        SpanData unmatched = extract(capture("mongo.request_id", "999", "find", encoded("{\"ok\":1}")));
        assertNull(unmatched.getInputData());
        assertNull(unmatched.getOutputData());

        SpanData requestOnly = extract(
                capture("mongo.request_id", "107", "find", encoded("{\"find\":\"products\"}")),
                capture("mongo.response_to", "107", "find", null));
        assertNotNull(requestOnly.getInputData());
        assertNull(requestOnly.getOutputData());
    }

    @Test
    void preservesEveryMatchedPayloadWhenMultipleCapturesMatch() throws Exception {
        SpanData data = extract(
                capture("mongo.response_to", "107", "find", encoded("{\"sku\":\"A\"}")),
                capture("mongo.response_to", "107", "find", encoded("{\"sku\":\"B\"}")));
        JsonNode results = tree(data.getOutputData()).path("results");
        assertEquals(2, results.size());
        assertEquals("A", results.get(0).path("payload").path("sku").asText());
        assertEquals("B", results.get(1).path("payload").path("sku").asText());
    }

    @Test
    void reportsMalformedPayloadWithoutLosingValidResponse() throws Exception {
        SpanData data = extract(
                capture("mongo.request_id", "107", "find", "!invalid-base64!"),
                capture("mongo.response_to", "107", "find", encoded("{\"ok\":1}")));
        JsonNode error = tree(data.getInputData()).path("payload_decode_error");
        assertTrue(error.isTextual());
        assertFalse(error.asText().isBlank());
        assertEquals(1, tree(data.getOutputData()).path("payload").path("ok").asInt());
    }

    private SpanData extract(DataObject... entries) {
        SimpleDataCollection captures = new SimpleDataCollection();
        for (DataObject entry : entries) captures.addDataEntry(entry);
        Span span = new Span("find", "span", "trace", null, "client", "product-service", 0, 100);
        span.getAttributes().put("db.mongodb.request_id", "106");
        span.getAttributes().put("db.operation.name", "find");
        return new MongoSpanDataExtractor(captures, new MongoPayloadDecoder()).extractData(span);
    }

    private DataObject capture(String idHeader, String id, String operation, String payload) {
        DataObject capture = new DataObject();
        capture.putValue("source", "mongo");
        capture.putValue("direction", idHeader.equals("mongo.request_id") ? "REQUEST" : "RESPONSE");
        capture.putValue("headers", new DataObject[]{header(idHeader, id), header("mongo.command", operation)});
        if (payload != null) capture.putValue("payload_inline", payload);
        return capture;
    }

    private DataObject header(String name, String value) {
        DataObject header = new DataObject();
        header.putValue("name", name);
        header.putValue("value", value);
        return header;
    }

    private String encoded(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private String opMsg(int requestId, int responseTo, String document) {
        var bson = RawBsonDocument.parse(document).getByteBuffer();
        byte[] bytes = new byte[bson.remaining()];
        bson.get(bytes);
        ByteBuffer message = ByteBuffer.allocate(21 + bytes.length).order(ByteOrder.LITTLE_ENDIAN);
        message.putInt(message.capacity()).putInt(requestId).putInt(responseTo).putInt(2013);
        message.putInt(0).put((byte) 0).put(bytes);
        return Base64.getEncoder().encodeToString(message.array());
    }

    private JsonNode tree(DataObject data) throws Exception {
        return json.readTree(data.toJson());
    }
}
