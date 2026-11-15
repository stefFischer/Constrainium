package at.scch.traceon.otel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.RawBsonDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class MongoPayloadDecoderTest {
    private final MongoPayloadDecoder decoder = new MongoPayloadDecoder();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void detectsJsonWithLeadingWhitespaceAndStandaloneBson() throws Exception {
        String document = "{\"sku\":\"STRW-001\"}";
        assertEquals(json.readTree(document), decode((" \n" + document).getBytes(StandardCharsets.UTF_8)));
        assertEquals(json.readTree("[" + document + "]"), decode(("[" + document + "]").getBytes(StandardCharsets.UTF_8)));
        assertEquals(json.readTree(document), decode(bson(document)));
    }

    @Test
    void readsAllOpMsgSectionsAndExcludesChecksumBytes() throws Exception {
        ByteBuffer message = message(2013);
        message.putInt(1); // checksumPresent
        message.put((byte) 0).put(bson("{\"insert\":\"products\"}"));
        message.put((byte) 1);
        int sectionStart = message.position();
        message.putInt(0);
        cstring(message, "documents");
        message.put(bson("{\"sku\":\"A\"}")).put(bson("{\"sku\":\"B\"}"));
        message.putInt(sectionStart, message.position() - sectionStart);
        message.putInt(0x12345678); // Not BSON; must be excluded from section parsing.
        assertEquals(json.readTree("[{\"insert\":\"products\"},{\"sku\":\"A\"},{\"sku\":\"B\"}]"), decode(finish(message)));
    }

    @Test
    void readsOpReplyDocumentCountAndSkipsWireMetadata() throws Exception {
        ByteBuffer message = message(1);
        message.putInt(0).putLong(123456L).putInt(10).putInt(2);
        message.put(bson("{\"sku\":\"A\"}")).put(bson("{\"sku\":\"B\"}"));
        assertEquals(json.readTree("[{\"sku\":\"A\"},{\"sku\":\"B\"}]"), decode(finish(message)));
    }

    @Test
    void readsOpQueryCollectionAndOptionalProjection() throws Exception {
        ByteBuffer message = message(2004);
        message.putInt(0);
        cstring(message, "shop.pro\"ducts"); // Exercises escaping in our JSON construction.
        message.putInt(5).putInt(10);
        message.put(bson("{\"sku\":\"A\"}")).put(bson("{\"sku\":1}"));
        assertEquals(json.readTree("{\"collection\":\"shop.pro\\\"ducts\",\"documents\":[{\"sku\":\"A\"},{\"sku\":1}]}"), decode(finish(message)));
    }

    @Test
    void rejectsCompressedMessagesWithAnActionableExplanation() {
        ByteBuffer message = message(2012);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> decode(finish(message)));
        assertTrue(error.getMessage().contains("OP_COMPRESSED"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4, 1000})
    void rejectsBsonLengthsOutsideTheMessageBoundary(int length) {
        ByteBuffer message = message(2013);
        message.putInt(0).put((byte) 0).putInt(length).put((byte) 0);
        assertThrows(IllegalArgumentException.class, () -> decode(finish(message)));
    }

    @Test
    void rejectsInvalidDocumentSequenceBoundariesAndUnsupportedSectionKinds() {
        ByteBuffer sequence = message(2013);
        sequence.putInt(0).put((byte) 1).putInt(1000).put((byte) 0);
        assertThrows(IllegalArgumentException.class, () -> decode(finish(sequence)));
        ByteBuffer unsupported = message(2013);
        unsupported.putInt(0).put((byte) 2);
        assertThrows(IllegalArgumentException.class, () -> decode(finish(unsupported)));
    }

    @Test
    void rejectsNegativeReplyCountAndUnexpectedTrailingBytes() {
        ByteBuffer negative = message(1);
        negative.putInt(0).putLong(0).putInt(0).putInt(-1);
        assertThrows(IllegalArgumentException.class, () -> decode(finish(negative)));
        ByteBuffer trailing = message(1);
        trailing.putInt(0).putLong(0).putInt(0).putInt(0).put((byte) 99);
        assertThrows(IllegalArgumentException.class, () -> decode(finish(trailing)));
    }

    private ByteBuffer message(int opcode) {
        ByteBuffer buffer = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0).putInt(107).putInt(0).putInt(opcode);
        return buffer;
    }

    private byte[] finish(ByteBuffer message) {
        message.putInt(0, message.position());
        return Arrays.copyOf(message.array(), message.position());
    }

    private byte[] bson(String document) {
        var buffer = RawBsonDocument.parse(document).getByteBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private void cstring(ByteBuffer message, String value) {
        message.put(value.getBytes(StandardCharsets.UTF_8)).put((byte) 0);
    }

    private JsonNode decode(byte[] bytes) throws Exception {
        return json.readTree(decoder.decode(Base64.getEncoder().encodeToString(bytes)).toJson()).path("payload");
    }
}
