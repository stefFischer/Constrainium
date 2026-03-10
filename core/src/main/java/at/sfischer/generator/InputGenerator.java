package at.sfischer.generator;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataSchema;

import java.util.stream.Stream;

public interface InputGenerator {
    String getIdentifier();

    Stream<DataObject> generate(SimpleDataSchema schema);
}
