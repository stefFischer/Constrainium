package at.scch.traceon.otel;

import at.sfischer.constraints.data.DataObject;

public interface PayloadDecoder {

    DataObject decode(String payload);
}
