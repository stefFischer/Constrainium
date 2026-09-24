package at.sfischer.generator;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.data.SimpleDataSchema;

import java.util.stream.Collector;
import java.util.stream.Stream;

public interface InputGenerator {
    String getIdentifier();

    Stream<DataObject> generate(SimpleDataSchema schema);

    default SimpleDataCollection generateData(SimpleDataSchema schema, int n){
        SimpleDataCollection collection = new SimpleDataCollection();
        Stream<DataObject> stream = generate(schema);
        stream.limit(n).forEach(collection::addDataEntry);
        return collection;
    }
}
