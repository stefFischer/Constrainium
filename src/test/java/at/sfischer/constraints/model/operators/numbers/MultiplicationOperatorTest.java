package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.numbers.MultiplicationOperator;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class MultiplicationOperatorTest {
	@Test
	public void simpleEvaluateTest() {
		Node left = new NumberLiteral(4);
		Node right = new NumberLiteral(4);
		MultiplicationOperator operator = new MultiplicationOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(NumberLiteral.class,result);
		assertEquals(16.0, ((NumberLiteral)result).getValue());
	}

	@Test
	public void evaluateTest() {
		MultiplicationOperator operator = new MultiplicationOperator(new MultiplicationOperator(new NumberLiteral(2), new NumberLiteral(2)), new NumberLiteral(2));
		Node result = operator.evaluate();

		assertInstanceOf(NumberLiteral.class,result);
		assertEquals(8.0, ((NumberLiteral)result).getValue());
	}

	@Test
	public void partiallyEvaluateTest() {
		MultiplicationOperator operator = new MultiplicationOperator(new MultiplicationOperator(new NumberLiteral(4), new NumberLiteral(4)), new Variable("x"));
		MultiplicationOperator expected = new MultiplicationOperator(new NumberLiteral(16.0), new Variable("x"));
		Node actual = operator.evaluate();

		assertInstanceOf(MultiplicationOperator.class, actual);
		assertThat(actual)
				.usingRecursiveComparison()
				.isEqualTo(expected);
	}

	@Test
	public void evaluateSimplificationTest1() {
		Node left = new Variable("a");
		Node right = new NumberLiteral(0);
		MultiplicationOperator operator = new MultiplicationOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(NumberLiteral.class,result);
		assertEquals(new NumberLiteral(0), result);
	}

	@Test
	public void evaluateSimplificationTest2() {
		Node left = new NumberLiteral(0);
		Node right = new Variable("a");
		MultiplicationOperator operator = new MultiplicationOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(NumberLiteral.class,result);
		assertEquals(new NumberLiteral(0), result);
	}
}
