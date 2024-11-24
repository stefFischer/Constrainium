package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.StringLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class StringMatchesTest {
	@Test
	public void evaluateTrueTest() {
		Node left = new StringLiteral("arun32");
		Node right = new StringLiteral("[a-zA-Z0-9]{6}");
		StringMatches operator = new StringMatches(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseTest() {
		Node left = new StringLiteral("kkvarun32");
		Node right = new StringLiteral("[a-zA-Z0-9]{6}");
		StringMatches operator = new StringMatches(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}
}