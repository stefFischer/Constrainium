package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.model.DataReference;
import at.sfischer.constraints.model.Node;

import at.sfischer.constraints.model.StringLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOrEqualOperator;
import at.sfischer.constraints.model.operators.objects.Reference;
import org.junit.jupiter.api.*;

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

		DataSchema.fillSchemaWithConstraints(term, schema.getDataSchemaEntries());

		Constraint constraint1 = new Constraint(new GreaterThanOperator(new DataReference(size), new DataReference(objectId)));
		Constraint constraint2 = new Constraint(new GreaterThanOperator(new DataReference(objectId), new DataReference(size)));

		assertEquals(2, size.potentialConstraints.size());
		assertTrue(size.potentialConstraints.contains(constraint1));
		assertTrue(size.potentialConstraints.contains(constraint2));

		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{id:10, value:\"string\"}}",
				"{size:1, isEmpty:false, object:{id:11, value:\"string\"}}",
				"{size:3, isEmpty:false, object:{id:12, value:\"string\"}}"
		);

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

		DataSchema.fillSchemaWithConstraints(term, schema.getDataSchemaEntries());

		Constraint constraint1 = new Constraint(new ForAll(new DataReference(array), new GreaterThanOrEqualOperator(new DataReference(size), new Variable(ArrayQuantifier.ELEMENT_NAME))));
		Constraint constraint2 = new Constraint(new ForAll(new DataReference(array), new GreaterThanOrEqualOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new DataReference(size))));

		assertEquals(1, size.potentialConstraints.size());
		assertEquals(constraint1, size.potentialConstraints.iterator().next());

		assertEquals(1, array.potentialConstraints.size());
		assertEquals(constraint2, array.potentialConstraints.iterator().next());

		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:1, array:[1]}",
				"{size:2, array:[1, 2]}",
				"{size:3, array:[1, 2, 3]}"
		);

		EvaluationResults<SimpleDataSchema, DataObject> results = schema.evaluate(data);

		assertEquals(0, results.getEvaluationResults().size());

		ConstraintResults<DataObject> constraintResults1 = results.getPotentialConstraintResults(size, constraint1, data);
		assertEquals(1.0, constraintResults1.applicationRate());
		assertFalse(constraintResults1.foundCounterExample());

		ConstraintResults<DataObject> constraintResults2 = results.getPotentialConstraintResults(array, constraint2, data);
		assertEquals(1.0/3.0, constraintResults2.applicationRate());
		assertTrue(constraintResults2.foundCounterExample());
	}

	@Test
	public void fillAndEvaluateSchemaComplexArrayTest1() {
		SimpleDataSchema schema = new SimpleDataSchema();
		DataSchemaEntry<SimpleDataSchema> size = schema.numberEntry("size", true);
		DataSchemaEntry<SimpleDataSchema> array = schema.objectArrayEntry("array", true);

		array.dataSchema.numberEntry("value",true);

		Node term = new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b"));

		DataSchema.fillSchemaWithConstraints(term, schema.getDataSchemaEntries());

		Constraint constraint1 = new Constraint(new ForAll(new DataReference(array), new GreaterThanOrEqualOperator(new DataReference(size), new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("value")))));
		Constraint constraint2 = new Constraint(new ForAll(new DataReference(array), new GreaterThanOrEqualOperator(new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("value")), new DataReference(size))));

		assertEquals(1, size.potentialConstraints.size());
		assertEquals(constraint1, size.potentialConstraints.iterator().next());

		assertEquals(1, array.potentialConstraints.size());
		assertEquals(constraint2, array.potentialConstraints.iterator().next());

		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:1, array:[{value: 1}]}",
				"{size:2, array:[{value: 1}, {value: 2}]}",
				"{size:3, array:[{value: 1}, {value: 2}, {value: 3}]}"
		);

		EvaluationResults<SimpleDataSchema, DataObject> results = schema.evaluate(data);

		assertEquals(0, results.getEvaluationResults().size());

		ConstraintResults<DataObject> constraintResults1 = results.getPotentialConstraintResults(size, constraint1, data);
		assertEquals(1.0, constraintResults1.applicationRate());
		assertFalse(constraintResults1.foundCounterExample());

		ConstraintResults<DataObject> constraintResults2 = results.getPotentialConstraintResults(array, constraint2, data);
		assertEquals(1.0/3.0, constraintResults2.applicationRate());
		assertTrue(constraintResults2.foundCounterExample());
	}

	// TODO Tests with InOutput schema.

}
