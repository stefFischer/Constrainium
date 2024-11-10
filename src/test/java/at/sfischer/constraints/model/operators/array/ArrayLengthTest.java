package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.miner.ConstraintMiner;
import at.sfischer.constraints.miner.ConstraintMinerFromData;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
}
