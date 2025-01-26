package at.sfischer.constraints.data;

import org.javatuples.Pair;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class InOutputDataCollectionTest {
	@Test
	public void deriveSchema() {
        InOutputDataCollection data = InOutputDataCollection.parseData(
                new Pair<>("{add:0}", "{size:3, object:{number:2}}"),
                new Pair<>("{add:5}", "{size:1, object:{number:0}}"),
                new Pair<>("{add:3}", "{size:2, object:{number:1}}")
        );

        SimpleDataSchema inputSchema = new SimpleDataSchema();
        inputSchema.numberEntry("add", true);

        SimpleDataSchema outputSchema = new SimpleDataSchema();
        outputSchema.numberEntry("size", true);
        SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> entry = outputSchema.objectEntry("object", true);
        entry.dataSchema.numberEntry("number",true);

        InOutputDataSchema<SimpleDataSchema> expected = new InOutputDataSchema<>(inputSchema, outputSchema);

        DataSchema actual = data.deriveSchema();

        assertEquals(expected, actual);
	}

    @Test
    public void deriveSchemaInconsistentFields() {
        InOutputDataCollection data = InOutputDataCollection.parseData(
                new Pair<>("{add:0}", "{size:3, object:{number:2}}"),
                new Pair<>("{add:5}", "{size:1}"),
                new Pair<>("{add:3}", "{size:2, object:{number:1}}")
        );

        SimpleDataSchema inputSchema = new SimpleDataSchema();
        inputSchema.numberEntry("add", true);

        SimpleDataSchema outputSchema = new SimpleDataSchema();
        outputSchema.numberEntry("size", true);
        SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> entry = outputSchema.objectEntry("object", false);
        entry.dataSchema.numberEntry("number",true);

        InOutputDataSchema<SimpleDataSchema> expected = new InOutputDataSchema<>(inputSchema, outputSchema);

        DataSchema actual = data.deriveSchema();

        assertEquals(expected, actual);
    }
}
