package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
import at.sfischer.constraints.model.operators.numbers.OneOfNumber;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ArrayIndexTest {
	@Test
	public void evaluateNumberArrayIndex() {
		Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(-1),
				new NumberLiteral(3),
				new NumberLiteral(1)
		});
		ArrayIndex operator = new ArrayIndex(array, new NumberLiteral(2));

		NumberLiteral expected = new NumberLiteral(3);
		Node result = operator.evaluate();

		assertEquals(expected, result);
	}

	@Test
	public void evaluateStringArrayIndex() {
		Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("first"),
				new StringLiteral("second"),
				new StringLiteral("third")
		});
		ArrayIndex operator = new ArrayIndex(array, new NumberLiteral(0));

		StringLiteral expected = new StringLiteral("first");
		Node result = operator.evaluate();

		assertEquals(expected, result);
	}

	@Test
	public void evaluateArrayIndexOutofBound() {
		Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("first"),
				new StringLiteral("second"),
				new StringLiteral("third")
		});
		ArrayIndex operator = new ArrayIndex(array, new NumberLiteral(3));

		Node result = operator.evaluate();

		assertEquals(operator, result);
	}

	@Test
	public void applyInSimpleDataSchema() {
		SimpleDataSchema schema = new SimpleDataSchema();

		DataSchemaEntry<SimpleDataSchema> objectEntry = schema.objectEntry("object", true);

		DataSchemaEntry<SimpleDataSchema> arrayEntry = objectEntry.dataSchema.numberArrayEntry("array", true);
		DataSchemaEntry<SimpleDataSchema> indexEntry = objectEntry.dataSchema.numberEntry("index", true);

		Constraint idConstraint = new Constraint(new OneOfNumber(new ArrayIndex(new DataReference(arrayEntry), new DataReference(indexEntry)), new NumberLiteral(1)));
		indexEntry.constraints.add(idConstraint);

		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{object: {array:[1,2,3], index: 1}}",
				"{object: {array:[1,0,2], index: 2}}",
				"{object: {array:[2,1,3], index: 0}}"
		);

		EvaluationResults<SimpleDataSchema, DataObject> actual = schema.evaluate(data);
		ConstraintResults<DataObject> results = actual.getConstraintResults(indexEntry, idConstraint, data);

		assertTrue(actual.getEvaluationResults().isEmpty());
		assertEquals(1.0, results.applicationRate());
		assertFalse(results.foundCounterExample());
	}
}
