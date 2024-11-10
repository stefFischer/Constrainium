package at.sfischer.constraints.miner;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.logic.OrOperator;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOrEqualOperator;
import at.sfischer.constraints.model.operators.numbers.LessThanOperator;
import at.sfischer.constraints.model.operators.numbers.LessThanOrEqualOperator;
import at.sfischer.constraints.model.operators.objects.Reference;
import org.javatuples.Pair;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ConstraintMinerTest {
	@Test
	public void getPossibleConstraintsTest1() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, isEmpty:true, object:{number:2}}",
				"{size:1, isEmpty:false, object:{number:3}}",
				"{size:3, isEmpty:false, object:{number:7}}"
		);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new NumberLiteral(0)));
		terms.add(new OrOperator(new LessThanOrEqualOperator(new Variable("a"), new NumberLiteral(0)), new NotOperator(new Variable("b"))));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new Variable("size"), new NumberLiteral(0)));
		expected.add(constraint1);
		Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new Variable("object.number"), new NumberLiteral(0)));
		expected.add(constraint2);
		Constraint constraint3 = new Constraint(new OrOperator(new LessThanOrEqualOperator(new Variable("size"), new NumberLiteral(0)), new NotOperator(new Variable("isEmpty"))));
		expected.add(constraint3);
		Constraint constraint4 = new Constraint(new OrOperator(new LessThanOrEqualOperator(new Variable("object.number"), new NumberLiteral(0)), new NotOperator(new Variable("isEmpty"))));
		expected.add(constraint4);

		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsTest2() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, object:{number:2}}",
				"{size:1, object:{number:3}}",
				"{size:3, object:{number:7}}"
		);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b")));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new GreaterThanOrEqualOperator(new Variable("size"), new Variable("object.number")));
		expected.add(constraint1);
		Constraint constraint2 = new Constraint(new GreaterThanOrEqualOperator(new Variable("object.number"), new Variable("size")));
		expected.add(constraint2);

		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsTest3() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, object:{number:2}, array:[1,2,3,4]}",
				"{size:1, object:{number:3}, array:[5,6,7,9]}",
				"{size:3, object:{number:7}, array:[0,1,2,3]}"
		);

		Set<Node> terms = new HashSet<>();
		terms.add(new ForAll(new Variable("a"), new LessThanOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new NumberLiteral(10))));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new ForAll(new Variable("array"), new LessThanOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new NumberLiteral(10))));
		expected.add(constraint1);

		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsTest4() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, object:{number:2}, array:[{number:1},{number:2},{number:3}]}",
				"{size:1, object:{number:3}, array:[{number:4},{number:5},{number:6}]}",
				"{size:3, object:{number:7}, array:[{number:7},{number:8},{number:9}]}"
		);

		Set<Node> terms = new HashSet<>();
		terms.add(new LessThanOperator(new Variable("a"), new NumberLiteral(10)));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new ForAll(new Variable("array"), new LessThanOperator(new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("number")), new NumberLiteral(10))));
		expected.add(constraint1);
		Constraint constraint2 = new Constraint(new LessThanOperator(new Variable("size"), new NumberLiteral(10)));
		expected.add(constraint2);
		Constraint constraint3 = new Constraint(new LessThanOperator(new Variable("object.number"), new NumberLiteral(10)));
		expected.add(constraint3);

		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsTestNestedObjectArrays() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{object:[{array:[{number:1},{number:2},{number:3}]}]}",
				"{object:[{array:[{number:4},{number:5},{number:6}]}]}",
				"{object:[{array:[{number:7},{number:8},{number:9}]}]}"
		);

		Set<Node> terms = new HashSet<>();
		terms.add(new LessThanOperator(new Variable("a"), new NumberLiteral(10)));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new ForAll(new Variable("object"), new ForAll(new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("array")), new LessThanOperator(new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("number")), new NumberLiteral(10)))));
		expected.add(constraint1);

		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsTestNestedArrays() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{array:[[1,2],[3,4]]}",
				"{array:[[5,6],[3,4]]}",
				"{array:[[7,8],[1,9]]}"
		);

		Set<Node> terms = new HashSet<>();
		terms.add(new ForAll(new Variable("a"), new LessThanOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new NumberLiteral(10))));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new ForAll(new Variable("array"), new ForAll(new Variable(ArrayQuantifier.ELEMENT_NAME), new LessThanOperator(new Variable(ArrayQuantifier.ELEMENT_NAME), new NumberLiteral(10)))));
		expected.add(constraint1);

		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsNoMatchingConstraintTest1() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{bool:true, string:\"Hello\"}",
				"{bool:true, string:\"World\"}",
				"{bool:false, string:\"!\"}"
		);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new NumberLiteral(0)));

		// We have no constraint matching the data, so it should return an empty set.
		Set<Constraint> expected = new HashSet<>();
		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

		assertEquals(expected, actual);
	}

	@Test
	public void getPossibleConstraintsNoMatchingConstraintTest2() {
		SimpleDataCollection data = SimpleDataCollection.parseData(
				"{size:0, object:{number:2}}",
				"{size:1, object:{number:3}}",
				"{size:3, object:{number:7}}"
		);

		Set<Node> terms = new HashSet<>();
		terms.add(new OrOperator(new LessThanOrEqualOperator(new Variable("a"), new NumberLiteral(0)), new NotOperator(new Variable("b"))));

		// We have no constraint matching the data, so it should return an empty set.
		Set<Constraint> expected = new HashSet<>();
		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

		assertEquals(expected, actual);
	}

	@Test
	public void getPossibleConstraintsTest5() {
		InOutputDataCollection data = InOutputDataCollection.parseData(
				new Pair<>("{add:0, object:{number:2}}", "{size:3}"),
				new Pair<>("{add:5, object:{number:0}}", "{size:1}"),
				new Pair<>("{add:3, object:{number:1}}", "{size:2}")
		);

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

		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

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

		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsTest7() {
		InOutputDataCollection data = InOutputDataCollection.parseData(
				new Pair<>("{size:0}", "{array:[{number:1},{number:2},{number:3}]}"),
				new Pair<>("{size:5}", "{array:[{number:1},{number:2},{number:3}]}"),
				new Pair<>("{size:3}", "{array:[{number:1},{number:2},{number:3}]}")
		);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b")));

		Set<Constraint> expected = new HashSet<>();
		Constraint constraint1 = new Constraint(new ForAll(new Variable("array"), new GreaterThanOrEqualOperator(new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("number")), new Variable("size"))));
		expected.add(constraint1);
		Constraint constraint2 = new Constraint(new ForAll(new Variable("array"), new GreaterThanOrEqualOperator(new Variable("size"), new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral("number")))));
		expected.add(constraint2);

		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

		assertThat(actual).
				usingRecursiveComparison().
				isEqualTo(expected);
	}

	@Test
	public void getPossibleConstraintsNoMatchingInputTest1() {
		InOutputDataCollection data = InOutputDataCollection.parseData(
				new Pair<>("{isEmpty:true}", "{size:3, object:{number:2}}"),
				new Pair<>("{isEmpty:false}", "{size:1, object:{number:0}}"),
				new Pair<>("{isEmpty:false}", "{size:2, object:{number:1}}")
		);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b")));

		// We have no constraint matching the data, so it should return an empty set.
		Set<Constraint> expected = new HashSet<>();
		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

		assertEquals(expected, actual);
	}

	@Test
	public void getPossibleConstraintsNoMatchingOutputTest1() {
		InOutputDataCollection data = InOutputDataCollection.parseData(
				new Pair<>("{size:3, object:{number:2}}", "{isEmpty:true}"),
				new Pair<>("{size:1, object:{number:0}}", "{isEmpty:false}"),
				new Pair<>("{size:2, object:{number:1}}", "{isEmpty:false}")
		);

		Set<Node> terms = new HashSet<>();
		terms.add(new GreaterThanOrEqualOperator(new Variable("a"), new Variable("b")));

		// We have no constraint matching the data, so it should return an empty set.
		Set<Constraint> expected = new HashSet<>();
		Set<Constraint> actual = ConstraintMiner.getPossibleConstraints(data, terms);

		assertEquals(expected, actual);
	}
}
