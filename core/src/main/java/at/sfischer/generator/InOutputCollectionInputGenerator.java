package at.sfischer.generator;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.SimpleDataSchema;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

public class InOutputCollectionInputGenerator implements InputGenerator {
    private final Iterator<DataObject> inputs;

    public InOutputCollectionInputGenerator(InOutputDataCollection dataCollection) {
        this.inputs = dataCollection.getDataCollection()
                .stream()
                .map(dataPair ->
                        (DataObject) dataPair
                                .getValue0()
                                .getDataValue("input")
                                .getValue()
                )
                .iterator();
    }

    @Override
    public String getIdentifier() {
        return "InOutputCollectionInputGenerator";
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
