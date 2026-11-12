package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.IConstraint;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
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

    @Test
    void filterKeepsMatchingEntries() {
        InOutputDataCollection data = InOutputDataCollection.parseData(
                new Pair<>("{value:1}", "{result:2}"),
                new Pair<>("{value:2}", "{result:3}"),
                new Pair<>("{value:3}", "{result:4}"),
                new Pair<>("{value:4}", "{result:1}")
        );

        IConstraint constraint = new Constraint(
                new GreaterThanOperator(
                        new Variable("output.result"),
                        new NumberLiteral(2)
                )
        );

        DataCollection<Pair<DataObject, DataObject>> filtered = data.filter(constraint);

        assertEquals(2, filtered.size());
    }

    @Test
    void filterReturnsEmptyCollectionWhenNothingMatches() {
        InOutputDataCollection data = InOutputDataCollection.parseData(
                new Pair<>("{value:1}", "{result:2}"),
                new Pair<>("{value:2}", "{result:3}")
        );

        IConstraint constraint = new Constraint(
                new GreaterThanOperator(
                        new Variable("output.result"),
                        new NumberLiteral(10)
                )
        );

        DataCollection<Pair<DataObject, DataObject>> filtered = data.filter(constraint);

        assertEquals(0, filtered.size());
    }

    @Test
    void filterReturnsAllEntriesWhenEverythingMatches() {
        InOutputDataCollection data = InOutputDataCollection.parseData(
                new Pair<>("{value:1}", "{result:2}"),
                new Pair<>("{value:2}", "{result:3}"),
                new Pair<>("{value:3}", "{result:4}")
        );

        IConstraint constraint = new Constraint(
                new GreaterThanOperator(
                        new Variable("output.result"),
                        new NumberLiteral(0)
                )
        );

        DataCollection<Pair<DataObject, DataObject>> filtered = data.filter(constraint);

        assertEquals(3, filtered.size());
    }

    @Test
    void filterCanUseInputAndOutputValues() {
        InOutputDataCollection data = InOutputDataCollection.parseData(
                new Pair<>("{value:1}", "{result:2}"),
                new Pair<>("{value:2}", "{result:3}"),
                new Pair<>("{value:3}", "{result:2}")
        );

        IConstraint constraint = new Constraint(
                new GreaterThanOperator(
                        new Variable("output.result"),
                        new Variable("input.value")
                )
        );

        DataCollection<Pair<DataObject, DataObject>> filtered = data.filter(constraint);

        assertEquals(2, filtered.size());
    }

    @Test
    void filterDoesNotModifyOriginalCollection() {
        InOutputDataCollection data = InOutputDataCollection.parseData(
                new Pair<>("{value:1}", "{result:2}"),
                new Pair<>("{value:2}", "{result:3}"),
                new Pair<>("{value:3}", "{result:4}")
        );

        IConstraint constraint = new Constraint(
                new GreaterThanOperator(
                        new Variable("output.result"),
                        new NumberLiteral(2)
                )
        );

        data.filter(constraint);

        assertEquals(3, data.size());
    }
}
