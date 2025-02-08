package at.sfischer.constraints.data;

import org.junit.jupiter.api.Test;

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
		expected.numberEntry("size", true);
		expected.booleanEntry("isEmpty", true);
		DataSchemaEntry<SimpleDataSchema> entry = expected.objectEntry("object", true);
		entry.dataSchema.numberEntry("number",true);

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
		expected.numberEntry("size", true);
		expected.booleanEntry("isEmpty", true);
		DataSchemaEntry<SimpleDataSchema> entry = expected.objectEntry("object", false);
		entry.dataSchema.numberEntry("number",true);

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

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.numberEntry("size", true);
		expected.booleanEntry("isEmpty", true);
		DataSchemaEntry<SimpleDataSchema> entry = expected.objectEntry("object", false);
		entry.dataSchema.numberEntry("number",true);

		IllegalStateException thrown = assertThrows(
				IllegalStateException.class,
                data::deriveSchema
		);

		assertTrue(thrown.getMessage().startsWith("Types for field \"isEmpty\" are not consistent:"));
	}
}
