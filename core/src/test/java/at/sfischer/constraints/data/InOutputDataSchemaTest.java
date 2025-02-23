package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.model.DataReference;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InOutputDataSchemaTest {

	@Test
	public void evaluateNothingWrongTest() {
		SimpleDataSchema inputSchema = new SimpleDataSchema();
		inputSchema.numberEntry("add", true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		outputSchema.numberEntry("size", true);
		DataSchemaEntry<SimpleDataSchema> entry = outputSchema.objectEntry("object", true);
		entry.dataSchema.numberEntry("number",true);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		@SuppressWarnings("unchecked")
		InOutputDataCollection data = InOutputDataCollection.parseData(
				new Pair<>("{add:0}", "{size:1, object:{number:0}}"),
				new Pair<>("{add:1}", "{size:1, object:{number:1}}"),
				new Pair<>("{add:2}", "{size:1, object:{number:2}}")
		);

		EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> expected = new EvaluationResults<>();
		EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> actual = schema.evaluate(data);

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateMissingMandatoryValueTest() {
		SimpleDataSchema inputSchema = new SimpleDataSchema();
		inputSchema.numberEntry("add", true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		outputSchema.numberEntry("size", true);
		DataSchemaEntry<SimpleDataSchema> objectEntry = outputSchema.objectEntry("object", true);
		objectEntry.dataSchema.numberEntry("number",true);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		@SuppressWarnings("unchecked")
		InOutputDataCollection data = InOutputDataCollection.parseData(
				new Pair<>("{add:0}", "{size:1, object:{number:0}}"),
				new Pair<>("{add:1}", "{size:1}"),
				new Pair<>("{add:2}", "{size:1, object:{number:2}}")
		);

		EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> expected = new EvaluationResults<>();
		expected.addResult(new MissingMandatoryValue<>(objectEntry, data.getDataCollection().get(1)));
		EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> actual = schema.evaluate(data);

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateWrongTypeTest() {
		SimpleDataSchema inputSchema = new SimpleDataSchema();
		inputSchema.numberEntry("add", true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		outputSchema.numberEntry("size", true);
		DataSchemaEntry<SimpleDataSchema> objectEntry = outputSchema.objectEntry("object", true);
		DataSchemaEntry<SimpleDataSchema> number = objectEntry.dataSchema.numberEntry("number",true);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		@SuppressWarnings("unchecked")
		InOutputDataCollection data = InOutputDataCollection.parseData(
				new Pair<>("{add:0}", "{size:1, object:{number:0}}"),
				new Pair<>("{add:1}", "{size:1, object:{number:true}}"),
				new Pair<>("{add:2}", "{size:1, object:{number:2}}")
		);

		EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> expected = new EvaluationResults<>();
		expected.addResult(new IncompatibleTypes<>(number, data.getDataCollection().get(1), TypeEnum.BOOLEAN));
		EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> actual = schema.evaluate(data);

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateNothingWrongWithConstraintsTest() {
		SimpleDataSchema inputSchema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> add = inputSchema.numberEntry("add", true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		outputSchema.numberEntry("size", true);
		DataSchemaEntry<SimpleDataSchema> objectEntry = outputSchema.objectEntry("object", true);
		DataSchemaEntry<SimpleDataSchema> number = objectEntry.dataSchema.numberEntry("number",true);
		Constraint numberConstraint = new Constraint(new EqualOperator(new DataReference(number), new DataReference(add)));
		number.constraints.add(numberConstraint);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		@SuppressWarnings("unchecked")
		InOutputDataCollection data = InOutputDataCollection.parseData(
				new Pair<>("{add:0}", "{size:1, object:{number:0}}"),
				new Pair<>("{add:1}", "{size:1, object:{number:1}}"),
				new Pair<>("{add:2}", "{size:1, object:{number:2}}")
		);

		EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> actual = schema.evaluate(data);
		ConstraintResults<Pair<DataObject, DataObject>> results = actual.getConstraintResults(number, numberConstraint, data);

		assertEquals(1.0, results.applicationRate());
		assertFalse(results.foundCounterExample());
	}

	@Test
	public void evaluateViolatedConstraintsTest() {
		SimpleDataSchema inputSchema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> add = inputSchema.numberEntry("add", true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		outputSchema.numberEntry("size", true);
		DataSchemaEntry<SimpleDataSchema> objectEntry = outputSchema.objectEntry("object", true);
		DataSchemaEntry<SimpleDataSchema> number = objectEntry.dataSchema.numberEntry("number",true);
		Constraint numberConstraint = new Constraint(new EqualOperator(new DataReference(number), new DataReference(add)));
		number.constraints.add(numberConstraint);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		@SuppressWarnings("unchecked")
		InOutputDataCollection data = InOutputDataCollection.parseData(
				new Pair<>("{add:0}", "{size:1, object:{number:0}}"),
				new Pair<>("{add:1}", "{size:1, object:{number:-1}}"),
				new Pair<>("{add:2}", "{size:1, object:{number:2}}")
		);

		EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> actual = schema.evaluate(data);
		ConstraintResults<Pair<DataObject, DataObject>> results = actual.getConstraintResults(number, numberConstraint, data);

		assertEquals(2.0/3.0, results.applicationRate());
		assertTrue(results.foundCounterExample());
	}
}
