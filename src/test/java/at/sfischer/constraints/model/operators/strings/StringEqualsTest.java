package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class StringEqualsTest {
	@Test
	public void evaluateTrueTest() {
		Node left = new StringLiteral("Hello");
		Node right = new StringLiteral("Hello");
		StringEquals operator = new StringEquals(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseTest() {
		Node left = new StringLiteral("Hello");
		Node right = new StringLiteral("Good bye");
		StringEquals operator = new StringEquals(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTautologyTest() {
		Node left = new Variable("a");
		Node right = new Variable("a");
		StringEquals operator = new StringEquals(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}
}
