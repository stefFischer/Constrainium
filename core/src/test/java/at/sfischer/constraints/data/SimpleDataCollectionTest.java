package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.IConstraint;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class SimpleDataCollectionTest {
	@Test
	public void deriveSchema() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{number:2}}",
				"{size:1, isEmpty:false, object:{number:3}}",
				"{size:3, isEmpty:false, object:{number:7}}"
		);

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.integerEntry("size", true);
		expected.booleanEntry("isEmpty", true);
		DataSchemaEntry<SimpleDataSchema> entry = expected.objectEntry("object", true);
		entry.dataSchema.integerEntry("number",true);

		DataSchema actual = data.deriveSchema();

		assertEquals(expected, actual);
	}

	@Test
	public void deriveSchemaInconsistentFields() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true}",
				"{size:1, isEmpty:false, object:{number:3}}",
				"{size:3, isEmpty:false, object:{number:7}}"
		);

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.integerEntry("size", true);
		expected.booleanEntry("isEmpty", true);
		DataSchemaEntry<SimpleDataSchema> entry = expected.objectEntry("object", false);
		entry.dataSchema.integerEntry("number",true);

		DataSchema actual = data.deriveSchema();

		assertEquals(expected, actual);
	}

	@Test
	public void deriveSchemaInconsistentTypes() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{number:2}}",
				"{size:1, isEmpty:\"NO\", object:{number:3}}",
				"{size:3, isEmpty:false, object:{number:7}}"
		);

		IllegalStateException thrown = assertThrows(
				IllegalStateException.class,
                data::deriveSchema
		);

		assertTrue(thrown.getMessage().startsWith("Types for field \"isEmpty\" are not consistent:"));
	}

	@Test
	public void deriveSchemaInconsistentTypesPromotionPolicyBooleanToString() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{number:2}}",
				"{size:1, isEmpty:\"NO\", object:{number:3}}",
				"{size:3, isEmpty:false, object:{number:7.0}}"
		);

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.integerEntry("size", true);
		expected.stringEntry("isEmpty", true);
		DataSchemaEntry<SimpleDataSchema> entry = expected.objectEntry("object", true);
		entry.dataSchema.numberEntry("number",true);

		DataSchema actual = data.deriveSchema(new DefaultTypePromotionPolicy());

		assertEquals(expected, actual);
	}

	@Test
	public void deriveSchemaInconsistentTypesPromotionPolicyNumberToString() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{number:2}}",
				"{size:1, isEmpty:true, object:{number:\"3\"}}",
				"{size:3, isEmpty:false, object:{number:7}}"
		);

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.integerEntry("size", true);
		expected.booleanEntry("isEmpty", true);
		DataSchemaEntry<SimpleDataSchema> entry = expected.objectEntry("object", true);
		entry.dataSchema.stringEntry("number",true);

		DataSchema actual = data.deriveSchema(new DefaultTypePromotionPolicy());

		assertEquals(expected, actual);
	}

	@Test
	public void jsonlRoundTrip() throws IOException {
		SimpleDataCollection original = SimpleDataCollection.parseData(
				"{value:1,name:\"one\"}",
				"{value:2,name:\"two\"}",
				"{value:3,name:\"three\"}"
		);

		File file = File.createTempFile("data-", ".jsonl");
		try {
			original.toJsonl(file);

			SimpleDataCollection parsed = SimpleDataCollection.parseData(file);

			assertThat(parsed).
					usingRecursiveComparison().
					isEqualTo(original);
		} finally {
			assertTrue(file.delete());
		}
	}

	@Test
	void filterKeepsMatchingEntries() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{value:1}",
				"{value:2}",
				"{value:3}",
				"{value:4}"
		);

		IConstraint constraint = new Constraint(
				new GreaterThanOperator(
						new Variable("value"),
						new NumberLiteral(2)
				)
		);

		DataCollection<DataObject> filtered = data.filter(constraint);

		assertEquals(2, filtered.size());
		assertEquals(
				SimpleDataCollection.parseData("{value:3}", "{value:4}"),
				filtered
		);
	}

	@Test
	void filterReturnsEmptyCollectionWhenNothingMatches() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{value:1}",
				"{value:2}"
		);

		IConstraint constraint = new Constraint(
				new GreaterThanOperator(
						new Variable("value"),
						new NumberLiteral(10)
				)
		);

		DataCollection<DataObject> filtered = data.filter(constraint);

        assertEquals(0, filtered.size());
	}

	@Test
	void filterReturnsAllEntriesWhenEverythingMatches() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{value:1}",
				"{value:2}",
				"{value:3}"
		);

		IConstraint constraint = new Constraint(
				new GreaterThanOperator(
						new Variable("value"),
						new NumberLiteral(0)
				)
		);

		DataCollection<DataObject> filtered = data.filter(constraint);

		assertEquals(data, filtered);
	}

	@Test
	void filterDoesNotModifyOriginalCollection() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{value:1}",
				"{value:2}",
				"{value:3}"
		);

		IConstraint constraint = new Constraint(
				new GreaterThanOperator(
						new Variable("value"),
						new NumberLiteral(1)
				)
		);

		data.filter(constraint);

		assertEquals(3, data.size());
	}
}
