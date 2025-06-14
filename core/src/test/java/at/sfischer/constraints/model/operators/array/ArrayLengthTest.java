package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.miner.ConstraintMiner;
import at.sfischer.constraints.miner.ConstraintMinerFromData;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOrEqualOperator;
import at.sfischer.constraints.model.operators.numbers.SubtractionOperator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ArrayLengthTest {
	@Test
	public void evaluateArrayLength() {
		Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(-1),
				new NumberLiteral(3),
				new NumberLiteral(1)
		});
		ArrayLength operator = new ArrayLength(array);

		Number expectedLength = 4;
		Node result = operator.evaluate();

		assertInstanceOf(NumberLiteral.class,result);
		assertEquals(expectedLength, ((NumberLiteral)result).getValue());
	}

	@Test
	public void getPossibleConstraints() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, object:{number:2}, array:[{number:1},{number:2},{number:3}]}",
				"{size:1, object:{number:3}, array:[{number:4},{number:5},{number:6}]}",
				"{size:3, object:{number:7}, array:[{number:7},{number:8},{number:9}]}"
		);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOperator(new ArrayLength(new Variable("a")), new NumberLiteral(1)));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new GreaterThanOperator(new ArrayLength(new Variable("array")), new NumberLiteral(1)));
		expected.add(constraint1);

		ConstraintMiner miner = new ConstraintMinerFromData(data);
		Set<Constraint> actual = miner.getPossibleConstraints(terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void applyData() {
		Constraint constraint = new Constraint(new GreaterThanOperator(new ArrayLength(new Variable("array")), new NumberLiteral(1)));
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, object:{number:2}, array:[{number:1},{number:2},{number:3}]}",
				"{size:1, object:{number:3}, array:[{number:4},{number:5},{number:6}]}",
				"{size:3, object:{number:7}, array:[{number:7},{number:8},{number:9}]}"
		);

		ConstraintResults expected = new ConstraintResults(constraint, data, data);
		ConstraintResults actual = constraint.applyData(data);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void testConsistencyDerivingEquivalentConstraints() {
		SimpleDataSchema schema = new SimpleDataSchema();

		DataSchemaEntry<SimpleDataSchema> dataEntry = schema.objectEntry("data", true);

		DataSchemaEntry<SimpleDataSchema> innerEntry = dataEntry.dataSchema.objectEntry("inner", true);
		DataSchemaEntry<SimpleDataSchema> array1Entry = innerEntry.dataSchema.stringArrayEntry("array1", true);

		DataSchemaEntry<SimpleDataSchema> otherEntry = dataEntry.dataSchema.objectEntry("other", true);
		DataSchemaEntry<SimpleDataSchema> deepEntry = otherEntry.dataSchema.objectEntry("deep", true);
		DataSchemaEntry<SimpleDataSchema> deeperEntry = deepEntry.dataSchema.objectEntry("deeper", true);
		DataSchemaEntry<SimpleDataSchema> array2Entry = deeperEntry.dataSchema.stringArrayEntry("array2", true);

		Node term1 = new GreaterThanOrEqualOperator(new ArrayLength(new Variable("a")), new ArrayLength(new Variable("b")));
		schema.fillSchemaWithConstraints(term1);

		Node term2 = new GreaterThanOrEqualOperator(new SubtractionOperator(new ArrayLength(new Variable("a")), new NumberLiteral(1)), new SubtractionOperator(new ArrayLength(new Variable("b")), new NumberLiteral(1)));
		schema.fillSchemaWithConstraints(term2);

		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{data:{inner:{array1:[\"ONE\", \"TWO\"]}, other:{deep:{deeper:{array2:[\"one\", \"two\"]}}}}}",
				"{data:{inner:{array1:[\"THREE\", \"TWO\"]}, other:{deep:{deeper:{array2:[\"three\", \"four\", \"one\"]}}}}}",
				"{data:{inner:{array1:[\"ONE\", \"THREE\"]}, other:{deep:{deeper:{array2:[\"one\", \"three\"]}}}}}"
		);

		EvaluationResults<SimpleDataSchema, DataObject> actual = schema.evaluate(data);
		assertTrue(actual.getEvaluationResults().isEmpty());

		Set<Constraint> constraints = array1Entry.potentialConstraints;
		assertEquals(4, constraints.size());

		Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new ArrayLength(new DataReference(array1Entry)), new ArrayLength(new DataReference(array2Entry))));
		Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new SubtractionOperator(new ArrayLength(new DataReference(array1Entry)), new NumberLiteral(1)), new SubtractionOperator(new ArrayLength(new DataReference(array2Entry)), new NumberLiteral(1))));
		Constraint constraint3 = new Constraint(new GreaterThanOrEqualOperator(new ArrayLength(new DataReference(array2Entry)), new ArrayLength(new DataReference(array1Entry))));
		Constraint constraint4 = new Constraint(new GreaterThanOrEqualOperator(new SubtractionOperator(new ArrayLength(new DataReference(array2Entry)), new NumberLiteral(1)), new SubtractionOperator(new ArrayLength(new DataReference(array1Entry)), new NumberLiteral(1))));

		assertThat(constraints)
				.contains(
						constraint1,
						constraint2,
						constraint3,
						constraint4
						);

		ConstraintResults<DataObject> results1_1 = actual.getPotentialConstraintResults(array1Entry, constraint1, data);
		ConstraintResults<DataObject> results1_2 = actual.getPotentialConstraintResults(array1Entry, constraint2, data);

		assertEquals(results1_1.applicationRate(), results1_2.applicationRate());
		assertEquals(2.0/3.0, results1_1.applicationRate());

		ConstraintResults<DataObject> results2_1 = actual.getPotentialConstraintResults(array1Entry, constraint3, data);
		ConstraintResults<DataObject> results2_2 = actual.getPotentialConstraintResults(array1Entry, constraint4, data);

		assertEquals(results2_1.applicationRate(), results2_2.applicationRate());
		assertEquals(1.0, results2_1.applicationRate());
	}
}
