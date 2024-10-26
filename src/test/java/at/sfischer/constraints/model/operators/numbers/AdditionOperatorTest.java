package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.numbers.AdditionOperator;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

public class AdditionOperatorTest {

	@Test
	public void simpleEvaluateTest() {
		Node left = new NumberLiteral(2);
		Node right = new NumberLiteral(2);
		AdditionOperator operator = new AdditionOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(NumberLiteral.class,result);
		assertEquals(4.0, ((NumberLiteral)result).getValue());
	}

	@Test
	public void evaluateTest() {
		AdditionOperator operator = new AdditionOperator(new AdditionOperator(new NumberLiteral(2), new NumberLiteral(2)), new NumberLiteral(2));
		Node result = operator.evaluate();

		assertInstanceOf(NumberLiteral.class,result);
		assertEquals(6.0, ((NumberLiteral)result).getValue());
	}

	@Test
	public void partiallyEvaluateTest() {
		AdditionOperator operator = new AdditionOperator(new AdditionOperator(new NumberLiteral(2), new NumberLiteral(2)), new Variable("x"));
		AdditionOperator expected = new AdditionOperator(new NumberLiteral(4.0), new Variable("x"));
		Node actual = operator.evaluate();

		assertInstanceOf(AdditionOperator.class, actual);
		assertThat(actual)
				.usingRecursiveComparison()
				.isEqualTo(expected);
	}

	@Test
	public void evaluateSimplificationTest1() {
		Node left = new Variable("a");
		Node right = new NumberLiteral(0);
		AdditionOperator operator = new AdditionOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(Variable.class,result);
		assertEquals(new Variable("a"), result);
	}

	@Test
	public void evaluateSimplificationTest2() {
		Node left = new NumberLiteral(0);
		Node right = new Variable("a");
		AdditionOperator operator = new AdditionOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(Variable.class,result);
		assertEquals(new Variable("a"), result);
	}
}
