package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class DivisionOperatorTest {
	@Test
	public void simpleEvaluateTest() {
		Node left = new NumberLiteral(16);
		Node right = new NumberLiteral(4);
		DivisionOperator operator = new DivisionOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(NumberLiteral.class,result);
		assertEquals(4.0, ((NumberLiteral)result).getValue());
	}

	@Test
	public void evaluateTest() {
		DivisionOperator operator = new DivisionOperator(new DivisionOperator(new NumberLiteral(2), new NumberLiteral(2)), new NumberLiteral(2));
		Node result = operator.evaluate();

		assertInstanceOf(NumberLiteral.class,result);
		assertEquals(0.5, ((NumberLiteral)result).getValue());
	}

	@Test
	public void partiallyEvaluateTest() {
		DivisionOperator operator = new DivisionOperator(new DivisionOperator(new NumberLiteral(16), new NumberLiteral(4)), new Variable("x"));
		DivisionOperator expected = new DivisionOperator(new NumberLiteral(4.0), new Variable("x"));
		Node actual = operator.evaluate();

		assertInstanceOf(DivisionOperator.class, actual);
		assertThat(actual)
				.usingRecursiveComparison()
				.isEqualTo(expected);
	}

	@Test
	public void evaluateSimplificationTest1() {
		Node left = new NumberLiteral(0);
		Node right = new Variable("a");
		DivisionOperator operator = new DivisionOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(NumberLiteral.class,result);
		assertEquals(new NumberLiteral(0), result);
	}
}
