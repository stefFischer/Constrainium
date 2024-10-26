package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.numbers.AdditionOperator;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOperator;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class GreaterThanOperatorTest {
	@Test
	public void evaluateTrueTest() {
		Node left = new NumberLiteral(4);
		Node right = new NumberLiteral(2);
		GreaterThanOperator operator = new GreaterThanOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseTest() {
		Node left = new NumberLiteral(2);
		Node right = new NumberLiteral(4);
		GreaterThanOperator operator = new GreaterThanOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseEqualTest() {
		Node left = new NumberLiteral(2);
		Node right = new NumberLiteral(2);
		GreaterThanOperator operator = new GreaterThanOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void partiallyEvaluateTest() {
		GreaterThanOperator operator = new GreaterThanOperator(new AdditionOperator(new NumberLiteral(2), new NumberLiteral(2)), new Variable("x"));
		GreaterThanOperator expected = new GreaterThanOperator(new NumberLiteral(4.0), new Variable("x"));
		Node actual = operator.evaluate();

		assertInstanceOf(GreaterThanOperator.class, actual);
		assertThat(actual)
				.usingRecursiveComparison()
				.isEqualTo(expected);
	}

	@Test
	public void evaluateContradictionTest() {
		Node left = new Variable("a");
		Node right = new Variable("a");
		GreaterThanOperator operator = new GreaterThanOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}
}
