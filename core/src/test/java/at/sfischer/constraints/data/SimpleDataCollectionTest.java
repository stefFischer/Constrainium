package at.sfischer.constraints.data;

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
}
