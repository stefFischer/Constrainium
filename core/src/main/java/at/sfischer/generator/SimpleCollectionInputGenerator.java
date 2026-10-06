package at.sfischer.generator;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.data.SimpleDataSchema;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

public class SimpleCollectionInputGenerator implements InputGenerator {
    private final Iterator<DataObject> inputs;

    public SimpleCollectionInputGenerator(SimpleDataCollection dataCollection) {
        this.inputs = dataCollection.getDataCollection()
                .stream()
                .iterator();
    }

    @Override
    public String getIdentifier() {
        return "SimpleCollectionInputGenerator";
    }

    @Override
    public Stream<DataObject> generate(SimpleDataSchema schema) {
        return Stream.generate(this::generateInput)
                .takeWhile(Objects::nonNull);
    }

    private DataObject generateInput() {
        if (!inputs.hasNext()) {
            return null;
        }

        return inputs.next();
    }
}
