package at.scch.traceon.otel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpPayloadDecoderTest {
    @Test
    void decodesUtf8AndWrapsTheWholeBodyUnderPayload() throws Exception {
        // Our responsibility is UTF-8 conversion and wrapping, not JSON parsing itself.
        String body = "[{\"name\":\"Grün\",\"payload\":\"business field\"}]";
        String encoded = Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));
        ObjectMapper json = new ObjectMapper();
        assertEquals(json.readTree("{\"payload\":" + body + "}"),
                json.readTree(new HttpPayloadDecoder().decode(encoded).toJson()));
    }
}
