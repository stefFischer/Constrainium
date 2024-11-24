package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.StringLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class IsDateTimeTest {
	@Test
	public void evaluateTrue1() {
		IsDateTime i = new IsDateTime(new StringLiteral("2024-01-01T12:00:00"));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrue2() {
		IsDateTime i = new IsDateTime(new StringLiteral("2024-01-01T12:00:00.000"));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalse() {
		IsDateTime i = new IsDateTime(new StringLiteral("01.01.2024 12:00:00"));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}
}
