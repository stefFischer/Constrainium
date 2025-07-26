package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.model.DataReference;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.array.ArrayLength;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOrEqualOperator;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DataSchemaTest {
	@Test
	public void fillAndEvaluateSchemaTest1() {
		SimpleDataSchema schema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> size = schema.numberEntry("size", true);
		schema.booleanEntry("isEmpty", true);

		DataSchemaEntry<SimpleDataSchema> entry = schema.objectEntry("object", true);
		DataSchemaEntry<SimpleDataSchema> objectId = entry.dataSchema.numberEntry("id",true);
		entry.dataSchema.stringEntry("value",true);

		Node term = new GreaterThanOperator(new Variable("a"), new Variable("b"));

		schema.fillSchemaWithConstraints(term);

		Constraint constraint1 = new Constraint(new GreaterThanOperator(new DataReference(size), new DataReference(objectId)));
		Constraint constraint2 = new Constraint(new GreaterThanOperator(new DataReference(objectId), new DataReference(size)));

		assertEquals(2, size.potentialConstraints.size());
		assertTrue(size.potentialConstraints.contains(constraint1));
		assertTrue(size.potentialConstraints.contains(constraint2));

		SimpleDataCollection data = SimpleDataCollection.parseData(
		  "{size:0, isEmpty:true, object:{id:10, value:\"string\"}}",
		  "{size:1, isEmpty:false, object:{id:11, value:\"string\"}}",
		  "{size:3, isEmpty:false, object:{id:12, value:\"string\"}}");


		EvaluationResults<SimpleDataSchema, DataObject> results = schema.evaluate(data);

		assertEquals(0, results.getEvaluationResults().size());

		ConstraintResults<DataObject> constraintResults1 = results.getPotentialConstraintResults(size, constraint1, data);
		assertEquals(0.0, constraintResults1.applicationRate());
		assertTrue(constraintResults1.foundCounterExample());

		ConstraintResults<DataObject> constraintResults2 = results.getPotentialConstraintResults(size, constraint2, data);
		assertEquals(1.0, constraintResults2.applicationRate());
		assertFalse(constraintResults2.foundCounterExample());
	}

	@Test
	public void fillAndEvaluateSchemaArrayTest1() {
		SimpleDataSchema schema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> size = schema.numberEntry("size", true);
		DataSchemaEntry<SimpleDataSchema> array = schema.numberArrayEntry("array", true);

		Node term = new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b"));

		schema.fillSchemaWithConstraints(term);

		Constraint constraint1 = new Constraint(new ForAll(new DataReference(array), new GreaterThanOrEqualOperator(new DataReference(size), new Variable(ArrayQuantifier.ELEMENT_NAME))));
		Constraint constraint2 = new Constraint(new ForAll(new DataReference(array), new GreaterThanOrEqualOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new DataReference(size))));

		assertEquals(1, size.potentialConstraints.size());
		assertEquals(constraint1, size.potentialConstraints.iterator().next());

		assertEquals(1, array.potentialConstraints.size());
		assertEquals(constraint2, array.potentialConstraints.iterator().next());

		SimpleDataCollection data = SimpleDataCollection.parseData(
		  "{size:1, array:[1]}",
		  "{size:2, array:[1, 2]}",
		  "{size:3, array:[1, 2, 3]}");


		EvaluationResults<SimpleDataSchema, DataObject> results = schema.evaluate(data);

		assertEquals(0, results.getEvaluationResults().size());

		ConstraintResults<DataObject> constraintResults1 = results.getPotentialConstraintResults(size, constraint1, data);
		assertEquals(1.0, constraintResults1.applicationRate());
		assertFalse(constraintResults1.foundCounterExample());

		ConstraintResults<DataObject> constraintResults2 = results.getPotentialConstraintResults(array, constraint2, data);
		assertEquals(1.0 / 3.0,constraintResults2.applicationRate());
		assertTrue(constraintResults2.foundCounterExample());
	}

	@Test
	public void fillAndEvaluateSchemaComplexArrayTest1() {
		SimpleDataSchema schema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> size = schema.numberEntry("size", true);
		DataSchemaEntry<SimpleDataSchema> array = schema.objectArrayEntry("array", true);

		DataSchemaEntry<SimpleDataSchema> arrayValue = array.dataSchema.numberEntry("value",true);

		Node term = new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b"));

		schema.fillSchemaWithConstraints(term);

		Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(size), new DataReference(arrayValue)));
		Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(arrayValue), new DataReference(size)));

		assertEquals(2, size.potentialConstraints.size());
		assertTrue(size.potentialConstraints.contains(constraint1));
		assertTrue(size.potentialConstraints.contains(constraint2));

		SimpleDataCollection data = SimpleDataCollection.parseData(
		  "{size:1, array:[{value: 1}]}",
		  "{size:2, array:[{value: 1}, {value: 2}]}",
		  "{size:3, array:[{value: 1}, {value: 2}, {value: 3}]}");


		EvaluationResults<SimpleDataSchema, DataObject> results = schema.evaluate(data);

		assertEquals(0, results.getEvaluationResults().size());

		ConstraintResults<DataObject> constraintResults1 = results.getPotentialConstraintResults(size, constraint1, data);
		assertEquals(1.0, constraintResults1.applicationRate());
		assertFalse(constraintResults1.foundCounterExample());

		ConstraintResults<DataObject> constraintResults2 = results.getPotentialConstraintResults(size, constraint2, data);
		assertEquals(1.0 / 3.0,constraintResults2.applicationRate());
		assertTrue(constraintResults2.foundCounterExample());
	}

	@Test
	public void fillAndEvaluateInOutputDataSchemaTest1() {
		SimpleDataSchema inputSchema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> add = inputSchema.numberEntry("add", true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> size = outputSchema.numberEntry("size", true);
		DataSchemaEntry<SimpleDataSchema> entry = outputSchema.objectEntry("object", true);
		DataSchemaEntry<SimpleDataSchema> number = entry.dataSchema.numberEntry("number",true);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		Node term = new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b"));

		schema.fillSchemaWithConstraints(term);

		Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(add), new DataReference(number)));
		Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(number), new DataReference(add)));
		Constraint constraint3 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(size), new DataReference(add)));
		Constraint constraint4 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(add), new DataReference(size)));

		assertEquals(2, number.potentialConstraints.size());
		assertTrue(number.potentialConstraints.contains(constraint1));
		assertTrue(number.potentialConstraints.contains(constraint2));

		assertEquals(2, size.potentialConstraints.size());
		assertTrue(size.potentialConstraints.contains(constraint3));
		assertTrue(size.potentialConstraints.contains(constraint4));

		InOutputDataCollection data = InOutputDataCollection.parseData(
		  new Pair<>("{add:0}", "{size:1, object:{number:0}}"),
		  new Pair<>("{add:1}", "{size:1, object:{number:1}}"),
		  new Pair<>("{add:2}", "{size:1, object:{number:2}}"));


		EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> results = schema.evaluate(data);

		assertEquals(0, results.getEvaluationResults().size());

		ConstraintResults<Pair<DataObject, DataObject>> constraintResults1 = results.getPotentialConstraintResults(number, constraint1, data);
		assertEquals(1.0, constraintResults1.applicationRate());
		assertFalse(constraintResults1.foundCounterExample());

		ConstraintResults<Pair<DataObject, DataObject>> constraintResults2 = results.getPotentialConstraintResults(number, constraint2, data);
		assertEquals(1.0, constraintResults2.applicationRate());
		assertFalse(constraintResults2.foundCounterExample());

		ConstraintResults<Pair<DataObject, DataObject>> constraintResults3 = results.getPotentialConstraintResults(size, constraint3, data);
		assertEquals(2.0 / 3.0,constraintResults3.applicationRate());
		assertTrue(constraintResults3.foundCounterExample());

		ConstraintResults<Pair<DataObject, DataObject>> constraintResults4 = results.getPotentialConstraintResults(size, constraint4, data);
		assertEquals(2.0 / 3.0,constraintResults4.applicationRate());
		assertTrue(constraintResults4.foundCounterExample());
	}

	@Test
	public void fillAndEvaluateInOutputDataSchemaArrayTest1() {
		SimpleDataSchema inputSchema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> size = inputSchema.numberEntry("size", true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> array = outputSchema.numberArrayEntry("array", true);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		Node term = new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b"));

		schema.fillSchemaWithConstraints(term);

		Constraint constraint1 = new Constraint(new ForAll(new DataReference(array), new GreaterThanOrEqualOperator(new DataReference(size), new Variable(ArrayQuantifier.ELEMENT_NAME))));
		Constraint constraint2 = new Constraint(new ForAll(new DataReference(array), new GreaterThanOrEqualOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new DataReference(size))));

		assertEquals(2, array.potentialConstraints.size());
		assertTrue(array.potentialConstraints.contains(constraint1));
		assertTrue(array.potentialConstraints.contains(constraint2));

		InOutputDataCollection data = InOutputDataCollection.parseData(
		  new Pair<>("{size:1}", "{array:[1]}"),
		  new Pair<>("{size:2}", "{array:[1,2]}"),
		  new Pair<>("{size:3}", "{array:[1,2,3]}"));


		EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> results = schema.evaluate(data);

		assertEquals(0, results.getEvaluationResults().size());

		ConstraintResults<Pair<DataObject, DataObject>> constraintResults1 = results.getPotentialConstraintResults(array, constraint1, data);
		assertEquals(1.0, constraintResults1.applicationRate());
		assertFalse(constraintResults1.foundCounterExample());

		ConstraintResults<Pair<DataObject, DataObject>> constraintResults2 = results.getPotentialConstraintResults(array, constraint2, data);
		assertEquals(1.0 / 3.0,constraintResults2.applicationRate());
		assertTrue(constraintResults2.foundCounterExample());

	}

	@Test
	public void fillAndEvaluateInOutputDataSchemaComplexArrayTest1() {
		SimpleDataSchema inputSchema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> size = inputSchema.numberEntry("size", true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> array = outputSchema.objectArrayEntry("array", true);
		DataSchemaEntry<SimpleDataSchema> arrayNumber = array.dataSchema.numberEntry("number",true);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		Node term = new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b"));

		schema.fillSchemaWithConstraints(term);

		Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(size), new DataReference(arrayNumber)));
		Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(arrayNumber), new DataReference(size)));

		assertEquals(2, arrayNumber.potentialConstraints.size());
		assertTrue(arrayNumber.potentialConstraints.contains(constraint1));
		assertTrue(arrayNumber.potentialConstraints.contains(constraint2));

		InOutputDataCollection data = InOutputDataCollection.parseData(
		  new Pair<>("{size:1}", "{array:[{number:1}]}"),
		  new Pair<>("{size:2}", "{array:[{number:1},{number:2}]}"),
		  new Pair<>("{size:3}", "{array:[{number:1},{number:2},{number:3}]}"));


		EvaluationResults<SimpleDataSchema, Pair<DataObject, DataObject>> results = schema.evaluate(data);

		assertEquals(0, results.getEvaluationResults().size());

		ConstraintResults<Pair<DataObject, DataObject>> constraintResults1 = results.getPotentialConstraintResults(arrayNumber, constraint1, data);
		assertEquals(1.0, constraintResults1.applicationRate());
		assertFalse(constraintResults1.foundCounterExample());

		ConstraintResults<Pair<DataObject, DataObject>> constraintResults2 = results.getPotentialConstraintResults(arrayNumber, constraint2, data);
		assertEquals(1.0 / 3.0,constraintResults2.applicationRate());
		assertTrue(constraintResults2.foundCounterExample());
	}

	@Test
	public void testFillSchemaWithConstraintsWithReplacementTerm() {
		SimpleDataSchema schema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> array = schema.objectArrayEntry("array", true);
		DataSchemaEntry<SimpleDataSchema> size = schema.numberEntry("size", true);

		ArrayLength replacement = new ArrayLength(new Variable("array"));

		Node term = new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b"));

		schema.fillSchemaWithConstraints(term, Set.of(replacement));

		Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new DataReference(size), new ArrayLength(new DataReference(array))));
		Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new ArrayLength(new DataReference(array)), new DataReference(size)));

		assertEquals(2, size.potentialConstraints.size());
		assertTrue(size.potentialConstraints.contains(constraint1));
		assertTrue(size.potentialConstraints.contains(constraint2));
	}
}
