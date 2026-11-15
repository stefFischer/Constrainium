package at.scch.traceon.otel;

import at.sfischer.constraints.data.DataObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HttpPayloadDecoder implements PayloadDecoder {

    @Override
    public DataObject decode(String payload) {
        return DataObject.parseData("{payload:" + new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8) + "}");
    }
}
