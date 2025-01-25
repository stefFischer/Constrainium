package at.sfischer.constraints.data;

import at.sfischer.constraints.model.ArrayType;
import at.sfischer.constraints.model.TypeEnum;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class SimpleDataSchemaTest {
	@Test
	public void deriveFromSimpleData() {
		String jsonData = "{size:0, isEmpty:true}";
		DataObject dao = DataObject.parseData(jsonData);

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.numberEntry("size", true);
		expected.booleanEntry("isEmpty", true);

		SimpleDataSchema actual = SimpleDataSchema.deriveFromData(dao);

		assertEquals(expected, actual);
	}

	@Test
	public void deriveFromNestedObjects() {
		String jsonData = "{size:1, isEmpty:false, object:{id:0, value:\"string\"}}";
		DataObject dao = DataObject.parseData(jsonData);

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.numberEntry("size", true);
		expected.booleanEntry("isEmpty", true);

		SimpleDataSchema.DataSchemaEntry entry = expected.objectEntry("object", true);
		entry.dataSchema.numberEntry("id",true);
		entry.dataSchema.stringEntry("value",true);

		SimpleDataSchema actual = SimpleDataSchema.deriveFromData(dao);

		assertEquals(expected, actual);
	}

	@Test
	public void deriveFromArrayData() {
		String jsonData = "{size:0, array:[1,2,3,4]}";
		DataObject dao = DataObject.parseData(jsonData);

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.numberEntry("size", true);
		expected.numberArrayEntry("array", true);

		SimpleDataSchema actual = SimpleDataSchema.deriveFromData(dao);

		assertEquals(expected, actual);
	}

	@Test
	public void deriveFromNestedArrayData() {
		String jsonData = "{size:0, array:[[1,2],[3,4]]}";
		DataObject dao = DataObject.parseData(jsonData);

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.numberEntry("size", true);
		expected.arrayEntryFor(new ArrayType(TypeEnum.NUMBER), "array", true);

		SimpleDataSchema actual = SimpleDataSchema.deriveFromData(dao);

		assertEquals(expected, actual);
	}

	@Test
	public void deriveFromComplexArrayData() {
		String jsonData = "{size:0, array:[{number:1},{number:2},{number:3}]}";
		DataObject dao = DataObject.parseData(jsonData);

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.numberEntry("size", true);
		SimpleDataSchema.DataSchemaEntry entry = expected.arrayEntryFor(TypeEnum.COMPLEXTYPE, "array", true);
		entry.dataSchema.numberEntry("number",true);

		SimpleDataSchema actual = SimpleDataSchema.deriveFromData(dao);

		assertEquals(expected, actual);
	}

	@Test
	public void deriveFromInconsistentComplexArrayData() {
		String jsonData = "{size:0, array:[{id:1, number:100},{id:2, number:200},{id:3}]}";
		DataObject dao = DataObject.parseData(jsonData);

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.numberEntry("size", true);
		SimpleDataSchema.DataSchemaEntry entry = expected.arrayEntryFor(TypeEnum.COMPLEXTYPE, "array", true);
		entry.dataSchema.numberEntry("id",true);
		entry.dataSchema.numberEntry("number",false);

		SimpleDataSchema actual = SimpleDataSchema.deriveFromData(dao);

		assertEquals(expected, actual);
	}

	@Test
	public void deriveFromNestedComplexArrayData() {
		String jsonData = "{size:0, array:[[{number:1},{number:2}],[{number:3}]]}";
		DataObject dao = DataObject.parseData(jsonData);

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.numberEntry("size", true);
		SimpleDataSchema.DataSchemaEntry entry = expected.arrayEntryFor(new ArrayType(TypeEnum.COMPLEXTYPE), "array", true);
		entry.dataSchema.numberEntry("number",true);

		SimpleDataSchema actual = SimpleDataSchema.deriveFromData(dao);

		assertEquals(expected, actual);
	}

	@Test
	public void deriveFromInconsistentNestedComplexArrayData() {
		String jsonData = "{size:0, array:[[{id:1, number:100},{id:2, number:200}],[{id:3}]]}";
		DataObject dao = DataObject.parseData(jsonData);

		SimpleDataSchema expected = new SimpleDataSchema();
		expected.numberEntry("size", true);
		SimpleDataSchema.DataSchemaEntry entry = expected.arrayEntryFor(new ArrayType(TypeEnum.COMPLEXTYPE), "array", true);
		entry.dataSchema.numberEntry("id",true);
		entry.dataSchema.numberEntry("number",false);

		SimpleDataSchema actual = SimpleDataSchema.deriveFromData(dao);

		assertEquals(expected, actual);
	}
}
