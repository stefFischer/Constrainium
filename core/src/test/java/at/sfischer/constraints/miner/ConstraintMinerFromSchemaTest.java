package at.sfischer.constraints.miner;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.InOutputDataSchema;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.Exists;
import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.logic.OrOperator;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOrEqualOperator;
import at.sfischer.constraints.model.operators.numbers.LessThanOperator;
import at.sfischer.constraints.model.operators.numbers.LessThanOrEqualOperator;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConstraintMinerFromSchemaTest {
	@Test
	public void getPossibleConstraintsTest1() {
		SimpleDataSchema schema = new SimpleDataSchema();
		schema.numberEntry("size", true);
		schema.booleanEntry("isEmpty", true);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new NumberLiteral(0)));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new Variable("size"), new NumberLiteral(0)));
		expected.add(constraint1);

		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertEquals(expected, actual);
	}

	@Test
	public void getPossibleConstraintsTest2() {
		SimpleDataSchema schema = new SimpleDataSchema();
		schema.numberEntry("size", true);
		SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> entry = schema.objectEntry("object", true);
		entry.dataSchema.numberEntry("number",true);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b")));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new Variable("size"), new Variable("object.number")));
		expected.add(constraint1);
		Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new Variable("object.number"), new Variable("size")));
		expected.add(constraint2);

		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertEquals(expected, actual);
	}

	@Test
	public void getPossibleConstraintsTest3() {
		SimpleDataSchema schema = new SimpleDataSchema();
		schema.numberEntry("size", true);
		SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> entry = schema.objectEntry("object", true);
		entry.dataSchema.numberEntry("number",true);
		schema.numberArrayEntry("array", true);

		Set<Node> terms = new HashSet<>();
		terms.add(new ForAll(new Variable("a"), new LessThanOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new NumberLiteral(10))));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new ForAll(new Variable("array"), new LessThanOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new NumberLiteral(10))));
		expected.add(constraint1);

		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsTest4() {
		SimpleDataSchema schema = new SimpleDataSchema();
		schema.numberEntry("size", true);
		SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> entry = schema.objectEntry("object", true);
		entry.dataSchema.numberEntry("number",true);
		SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> arrayEntry = schema.objectArrayEntry("array", true);
		arrayEntry.dataSchema.numberEntry("number",true);

		Set<Node> terms = new HashSet<>();
		terms.add(new LessThanOperator(new Variable("a"), new NumberLiteral(10)));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new ForAll(new Variable("array"), new LessThanOperator(new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("number")), new NumberLiteral(10))));
		expected.add(constraint1);
		Constraint constraint2 = new Constraint(new LessThanOperator(new Variable("size"), new NumberLiteral(10)));
		expected.add(constraint2);
		Constraint constraint3 = new Constraint(new LessThanOperator(new Variable("object.number"), new NumberLiteral(10)));
		expected.add(constraint3);

		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsTestNestedObjectArrays() {
		SimpleDataSchema schema = new SimpleDataSchema();
		SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> entry = schema.objectArrayEntry("object", true);
		SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> arrayEntry = entry.dataSchema.objectArrayEntry("array", true);
		arrayEntry.dataSchema.numberEntry("number",true);

		Set<Node> terms = new HashSet<>();
		terms.add(new LessThanOperator(new Variable("a"), new NumberLiteral(10)));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new ForAll(new Variable("object"), new ForAll(new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("array")), new LessThanOperator(new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("number")), new NumberLiteral(10)))));
		expected.add(constraint1);

		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsTestNestedArrays() {
		SimpleDataSchema schema = new SimpleDataSchema();
		schema.arrayEntryFor(new ArrayType(TypeEnum.NUMBER), "array", true);

		Set<Node> terms = new HashSet<>();
		terms.add(new ForAll(new Variable("a"), new LessThanOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new NumberLiteral(10))));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new ForAll(new Variable("array"), new ForAll(new Variable(ArrayQuantifier.ELEMENT_NAME), new LessThanOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new NumberLiteral(10)))));
		expected.add(constraint1);

		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsNoMatchingConstraintTest1() {
		SimpleDataSchema schema = new SimpleDataSchema();
		schema.booleanEntry("bool", true);
		schema.stringEntry("string", true);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new NumberLiteral(0)));

		// We have no constraint matching the data, so it should return an empty set.
		Set<Constraint> expected = new HashSet<>();
		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertEquals(expected, actual);
	}

	@Test
	public void getPossibleConstraintsNoMatchingConstraintTest2() {
		SimpleDataSchema schema = new SimpleDataSchema();
		schema.numberEntry("size", true);
		SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> entry = schema.objectEntry("object", true);
		entry.dataSchema.numberEntry("number",true);

		Set<Node> terms = new HashSet<>();
		terms.add(new OrOperator(new LessThanOrEqualOperator(new Variable("a"), new NumberLiteral(0)), new NotOperator(new Variable("b"))));

		// We have no constraint matching the data, so it should return an empty set.
		Set<Constraint> expected = new HashSet<>();
		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertEquals(expected, actual);
	}

	@Test
	public void getPossibleConstraintsStringMemberOfArray() {
		SimpleDataSchema schema = new SimpleDataSchema();
		schema.stringEntry("element", true);
		schema.stringArrayEntry("array", true);

		Set<Node> terms = new HashSet<>();
		terms.add(new Exists(new Variable("a"), new StringEquals(new Variable(ArrayQuantifier.ELEMENT_NAME), new Variable("b"))));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new Exists(new Variable("array"), new StringEquals(new Variable(ArrayQuantifier.ELEMENT_NAME), new Variable("element"))));
		expected.add(constraint1);

		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsTest5() {
		SimpleDataSchema inputSchema = new SimpleDataSchema();
		inputSchema.numberEntry("add", true);
		SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> entry = inputSchema.objectEntry("object", true);
		entry.dataSchema.numberEntry("number",true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		outputSchema.numberEntry("size", true);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b")));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new Variable("add"), new Variable("size")));
		expected.add(constraint1);
		Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new Variable("size"), new Variable("add")));
		expected.add(constraint2);
		Constraint constraint3 = new Constraint(new GreaterThanOrEqualOperator(new Variable("size"), new Variable("object.number")));
		expected.add(constraint3);
		Constraint constraint4 = new Constraint(new GreaterThanOrEqualOperator(new Variable("object.number"), new Variable("size")));
		expected.add(constraint4);

		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsTest6() {
		InOutputDataCollection data = InOutputDataCollection.parseData(
				new Pair<>("{add:0}", "{size:3, object:{number:2}}"),
				new Pair<>("{add:5}", "{size:1, object:{number:0}}"),
				new Pair<>("{add:3}", "{size:2, object:{number:1}}")
		);

		SimpleDataSchema inputSchema = new SimpleDataSchema();
		inputSchema.numberEntry("add", true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		outputSchema.numberEntry("size", true);
		SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> entry = outputSchema.objectEntry("object", true);
		entry.dataSchema.numberEntry("number",true);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b")));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new Variable("add"), new Variable("size")));
		expected.add(constraint1);
		Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new Variable("size"), new Variable("add")));
		expected.add(constraint2);
		Constraint constraint3 = new Constraint(new GreaterThanOrEqualOperator(new Variable("add"), new Variable("object.number")));
		expected.add(constraint3);
		Constraint constraint4 = new Constraint(new GreaterThanOrEqualOperator(new Variable("object.number"), new Variable("add")));
		expected.add(constraint4);

		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsTest7() {
		SimpleDataSchema inputSchema = new SimpleDataSchema();
		inputSchema.numberEntry("size", true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> arrayEntry = outputSchema.objectArrayEntry("array", true);
		arrayEntry.dataSchema.numberEntry("number",true);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b")));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new ForAll(new Variable("array"), new GreaterThanOrEqualOperator(new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("number")), new Variable("size"))));
		expected.add(constraint1);
		Constraint constraint2 = new Constraint(new ForAll(new Variable("array"), new GreaterThanOrEqualOperator(new Variable("size"), new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("number")))));
		expected.add(constraint2);

		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsNoMatchingInputTest1() {
		SimpleDataSchema inputSchema = new SimpleDataSchema();
		inputSchema.booleanEntry("isEmpty", true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		outputSchema.numberEntry("size", true);
		SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> entry = outputSchema.objectEntry("object", true);
		entry.dataSchema.numberEntry("number",true);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b")));

		// We have no constraint matching the data, so it should return an empty set.
		Set<Constraint> expected = new HashSet<>();
		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertEquals(expected, actual);
	}

	@Test
	public void getPossibleConstraintsNoMatchingOutputTest1() {
		SimpleDataSchema inputSchema = new SimpleDataSchema();
		inputSchema.numberEntry("size", true);
		SimpleDataSchema.DataSchemaEntry<SimpleDataSchema> entry = inputSchema.objectEntry("object", true);
		entry.dataSchema.numberEntry("number",true);

		SimpleDataSchema outputSchema = new SimpleDataSchema();
		outputSchema.booleanEntry("isEmpty", true);

		InOutputDataSchema<SimpleDataSchema> schema = new InOutputDataSchema<>(inputSchema, outputSchema);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b")));

		// We have no constraint matching the data, so it should return an empty set.
		Set<Constraint> expected = new HashSet<>();
		ConstraintMinerFromSchema c = new ConstraintMinerFromSchema(schema);
		Set<Constraint> actual = c.getPossibleConstraints(terms);

		assertEquals(expected, actual);
	}
}
