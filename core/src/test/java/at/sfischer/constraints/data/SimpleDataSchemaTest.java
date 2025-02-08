package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.model.ArrayType;
import at.sfischer.constraints.model.DataReference;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOrEqualOperator;
import org.junit.jupiter.api.Test;

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

		DataSchemaEntry<SimpleDataSchema> entry = expected.objectEntry("object", true);
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
		DataSchemaEntry<SimpleDataSchema> entry = expected.arrayEntryFor(TypeEnum.COMPLEXTYPE, "array", true);
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
		DataSchemaEntry<SimpleDataSchema> entry = expected.arrayEntryFor(TypeEnum.COMPLEXTYPE, "array", true);
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
		DataSchemaEntry<SimpleDataSchema> entry = expected.arrayEntryFor(new ArrayType(TypeEnum.COMPLEXTYPE), "array", true);
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
		DataSchemaEntry<SimpleDataSchema> entry = expected.arrayEntryFor(new ArrayType(TypeEnum.COMPLEXTYPE), "array", true);
		entry.dataSchema.numberEntry("id",true);
		entry.dataSchema.numberEntry("number",false);

		SimpleDataSchema actual = SimpleDataSchema.deriveFromData(dao);

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateNothingWrongTest() {
		SimpleDataSchema schema = new SimpleDataSchema();
		schema.numberEntry("size", true);
		schema.booleanEntry("isEmpty", true);

		DataSchemaEntry<SimpleDataSchema> entry = schema.objectEntry("object", true);
		entry.dataSchema.numberEntry("id",true);
		entry.dataSchema.stringEntry("value",true);

		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{id:0, value:\"string\"}}",
				"{size:1, isEmpty:false, object:{id:0, value:\"string\"}}",
				"{size:3, isEmpty:false, object:{id:0, value:\"string\"}}"
		);

		EvaluationResults<SimpleDataSchema, DataObject> expected = new EvaluationResults<>();
		EvaluationResults<SimpleDataSchema, DataObject> actual = schema.evaluate(data);

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateMissingMandatoryValueTest() {
		SimpleDataSchema schema = new SimpleDataSchema();
		schema.numberEntry("size", true);
		schema.booleanEntry("isEmpty", true);

		DataSchemaEntry<SimpleDataSchema> entry = schema.objectEntry("object", true);
		entry.dataSchema.numberEntry("id",true);
		DataSchemaEntry<SimpleDataSchema> valueEntry = entry.dataSchema.stringEntry("value",true);

		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{id:0, value:\"string\"}}",
				"{size:1, isEmpty:false, object:{id:0}}",
				"{size:3, isEmpty:false, object:{id:0, value:\"string\"}}"
		);

		EvaluationResults<SimpleDataSchema, DataObject> expected = new EvaluationResults<>();
		expected.addResult(new MissingMandatoryValue<>(valueEntry, data.getDataCollection().get(1)));
		EvaluationResults<SimpleDataSchema, DataObject> actual = schema.evaluate(data);

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateWrongTypeTest() {
		SimpleDataSchema schema = new SimpleDataSchema();
		schema.numberEntry("size", true);
		schema.booleanEntry("isEmpty", true);

		DataSchemaEntry<SimpleDataSchema> entry = schema.objectEntry("object", true);
		entry.dataSchema.numberEntry("id",true);
		DataSchemaEntry<SimpleDataSchema> valueEntry = entry.dataSchema.stringEntry("value",true);

		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{id:0, value:\"string\"}}",
				"{size:1, isEmpty:false, object:{id:0, value:false}}}",
				"{size:3, isEmpty:false, object:{id:0, value:\"string\"}}"
		);

		EvaluationResults<SimpleDataSchema, DataObject> expected = new EvaluationResults<>();
		expected.addResult(new IncompatibleTypes<>(valueEntry, data.getDataCollection().get(1), TypeEnum.BOOLEAN));
		EvaluationResults<SimpleDataSchema, DataObject> actual = schema.evaluate(data);

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateNothingWrongWithConstraintsTest() {
		SimpleDataSchema schema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> sizeEntry = schema.numberEntry("size", true);
		Constraint sizeConstraint = new Constraint(new GreaterThanOrEqualOperator(new DataReference(sizeEntry), new NumberLiteral(0)));
		sizeEntry.constraints.add(sizeConstraint);
		schema.booleanEntry("isEmpty", true);

		DataSchemaEntry<SimpleDataSchema> entry = schema.objectEntry("object", true);
		entry.dataSchema.numberEntry("id",true);
		entry.dataSchema.stringEntry("value",true);

		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{id:0, value:\"string\"}}",
				"{size:1, isEmpty:false, object:{id:0, value:\"string\"}}",
				"{size:3, isEmpty:false, object:{id:0, value:\"string\"}}"
		);

		EvaluationResults<SimpleDataSchema, DataObject> actual = schema.evaluate(data);
		ConstraintResults<DataObject> results = actual.getConstraintResults(sizeEntry, sizeConstraint, data);

		assertEquals(1.0, results.applicationRate());
		assertFalse(results.foundCounterExample());
	}

	@Test
	public void evaluateViolatedConstraintsTest() {
		SimpleDataSchema schema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> sizeEntry = schema.numberEntry("size", true);
		Constraint sizeConstraint = new Constraint(new GreaterThanOperator(new DataReference(sizeEntry), new NumberLiteral(0)));
		sizeEntry.constraints.add(sizeConstraint);
		schema.booleanEntry("isEmpty", true);

		DataSchemaEntry<SimpleDataSchema> entry = schema.objectEntry("object", true);
		entry.dataSchema.numberEntry("id",true);
		entry.dataSchema.stringEntry("value",true);

		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{id:0, value:\"string\"}}",
				"{size:1, isEmpty:false, object:{id:0, value:\"string\"}}",
				"{size:3, isEmpty:false, object:{id:0, value:\"string\"}}"
		);

		EvaluationResults<SimpleDataSchema, DataObject> actual = schema.evaluate(data);
		ConstraintResults<DataObject> results = actual.getConstraintResults(sizeEntry, sizeConstraint, data);

		assertEquals(2.0/3.0, results.applicationRate());
		assertTrue(results.foundCounterExample());
	}
}
