package at.scch.traceon.otel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RedisPayloadDecoderTest {
    private final RedisPayloadDecoder decoder = new RedisPayloadDecoder();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void countsBulkStringBytesRatherThanCharactersAndPreservesEmbeddedCrlf() throws Exception {
        assertEquals("Grün\r\n", payload("$7\r\nGrün\r\n\r\n").asText());
    }

    @Test
    void distinguishesASingleReplyFromConcatenatedReplies() throws Exception {
        assertEquals("OK", payload("+OK\r\n").asText());
        assertEquals(json.readTree("[\"OK\",\"QUEUED\"]"), payload("+OK\r\n+QUEUED\r\n"));
    }

    @Test
    void readsNestedArraysAndResp3Maps() throws Exception {
        assertEquals(json.readTree("[[1,2],[3,4]]"), payload("*2\r\n*2\r\n:1\r\n:2\r\n*2\r\n:3\r\n:4\r\n"));
        assertEquals(json.readTree("{\"available\":true,\"price\":7.29,\"stock\":219}"),
                payload("%3\r\n+available\r\n#t\r\n+price\r\n,7.29\r\n+stock\r\n:219\r\n"));
    }

    @Test
    void handlesSetsPushMessagesAndVerbatimStrings() throws Exception {
        assertEquals(json.readTree("[\"A\",\"B\"]"), payload("~2\r\n+A\r\n+B\r\n"));
        assertEquals(json.readTree("[\"message\",\"orders\",\"created\"]"),
                payload(">3\r\n+message\r\n+orders\r\n+created\r\n"));
        assertEquals("hello", payload("=9\r\ntxt:hello\r\n").asText());
    }

    @Test
    void preservesErrorTextAndLargeIntegerText() throws Exception {
        assertEquals("ERR failed", payload("-ERR failed\r\n").asText());
        assertEquals("ERR failed", payload("!10\r\nERR failed\r\n").asText());
        assertEquals("3492890328409238509324850943850943825024385",
                payload("(3492890328409238509324850943850943825024385\r\n").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"+unterminated", "$3\r\nab", "$3\r\nabcXX", "*2\r\n:1\r\n",
            "*-2\r\n", "%-2\r\n", "$-2\r\n", "$abc\r\n", "#invalid\r\n", "?unknown\r\n"})
    void rejectsMalformedProtocolFraming(String resp) {
        assertThrows(IllegalArgumentException.class, () -> decoder.decode(encoded(resp)));
    }

    private JsonNode payload(String resp) throws Exception {
        return json.readTree(decoder.decode(encoded(resp)).toJson()).path("payload");
    }

    private String encoded(String resp) {
        return Base64.getEncoder().encodeToString(resp.getBytes(StandardCharsets.UTF_8));
    }
}
