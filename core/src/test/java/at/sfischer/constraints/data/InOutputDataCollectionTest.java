package at.sfischer.constraints.data;

import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InOutputDataCollectionTest {
	@Test
	public void deriveSchema() {
        InOutputDataCollection data = InOutputDataCollection.parseData(
                new Pair<>("{add:0}", "{size:3, object:{number:2.0}}"),
                new Pair<>("{add:5}", "{size:1, object:{number:0.0}}"),
                new Pair<>("{add:3}", "{size:2, object:{number:1.0}}")
        );

        SimpleDataSchema inputSchema = new SimpleDataSchema();
        inputSchema.integerEntry("add", true);

        SimpleDataSchema outputSchema = new SimpleDataSchema();
        outputSchema.integerEntry("size", true);
        DataSchemaEntry<SimpleDataSchema> entry = outputSchema.objectEntry("object", true);
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
        inputSchema.integerEntry("add", true);

        SimpleDataSchema outputSchema = new SimpleDataSchema();
        outputSchema.integerEntry("size", true);
        DataSchemaEntry<SimpleDataSchema> entry = outputSchema.objectEntry("object", false);
        entry.dataSchema.integerEntry("number",true);

        InOutputDataSchema<SimpleDataSchema> expected = new InOutputDataSchema<>(inputSchema, outputSchema);

        DataSchema actual = data.deriveSchema();

        assertEquals(expected, actual);
    }

    @Test
    void inOutputJsonlRoundTrip() throws IOException {
        InOutputDataCollection original = InOutputDataCollection.parseData(
                new Pair<>("{value:1}", "{result:2}"),
                new Pair<>("{value:2}", "{result:3}"),
                new Pair<>("{value:3}",	"{result:4}")
        );

        File file = File.createTempFile("inout-", ".jsonl");
        try {
            original.toJsonl(file);

            InOutputDataCollection parsed = InOutputDataCollection.parseData(file);

            assertThat(parsed).
                    usingRecursiveComparison().
                    isEqualTo(original);
        } finally {
            assertTrue(file.delete());
        }
    }
}
