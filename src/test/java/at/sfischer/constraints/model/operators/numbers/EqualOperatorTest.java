package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class EqualOperatorTest {
	@Test
	public void evaluateTrueTest() {
		Node left = new NumberLiteral(2);
		Node right = new NumberLiteral(2);
		EqualOperator operator = new EqualOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseTest() {
		Node left = new NumberLiteral(2);
		Node right = new NumberLiteral(4);
		EqualOperator operator = new EqualOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTautologyTest() {
		Node left = new Variable("a");
		Node right = new Variable("a");
		EqualOperator operator = new EqualOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}
}
