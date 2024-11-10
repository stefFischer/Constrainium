package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.StringLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class IsDateTest {
	@Test
	public void evaluateTrue1() {
		IsDate i = new IsDate(new StringLiteral("2024-01-01"));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrue2() {
		IsDate i = new IsDate(new StringLiteral("2024/01/01"));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalse() {
		IsDate i = new IsDate(new StringLiteral("01.01.2024"));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}
}
