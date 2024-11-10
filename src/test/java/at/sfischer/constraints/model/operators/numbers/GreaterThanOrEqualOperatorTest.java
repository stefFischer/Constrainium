package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class GreaterThanOrEqualOperatorTest {
	@Test
	public void evaluateTrueTest() {
		Node left = new NumberLiteral(4);
		Node right = new NumberLiteral(2);
		GreaterThanOrEqualOperator operator = new GreaterThanOrEqualOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseTest() {
		Node left = new NumberLiteral(2);
		Node right = new NumberLiteral(4);
		GreaterThanOrEqualOperator operator = new GreaterThanOrEqualOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrueEqualTest() {
		Node left = new NumberLiteral(2);
		Node right = new NumberLiteral(2);
		GreaterThanOrEqualOperator operator = new GreaterThanOrEqualOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void partiallyEvaluateTest() {
		GreaterThanOrEqualOperator operator = new GreaterThanOrEqualOperator(new AdditionOperator(new NumberLiteral(2), new NumberLiteral(2)), new Variable("x"));
		GreaterThanOrEqualOperator expected = new GreaterThanOrEqualOperator(new NumberLiteral(4.0), new Variable("x"));
		Node actual = operator.evaluate();

		assertInstanceOf(GreaterThanOrEqualOperator.class, actual);
		assertThat(actual)
				.usingRecursiveComparison()
				.isEqualTo(expected);
	}

	@Test
	public void evaluateTautologyTest() {
		Node left = new Variable("a");
		Node right = new Variable("a");
		GreaterThanOrEqualOperator operator = new GreaterThanOrEqualOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}
}
