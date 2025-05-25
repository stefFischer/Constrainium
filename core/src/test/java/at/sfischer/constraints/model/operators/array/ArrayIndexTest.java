package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ArrayIndexTest {
	@Test
	public void evaluateNumberArrayIndex() {
		Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(-1),
				new NumberLiteral(3),
				new NumberLiteral(1)
		});
		ArrayIndex operator = new ArrayIndex(array, new NumberLiteral(2));

		NumberLiteral expected = new NumberLiteral(3);
		Node result = operator.evaluate();

		assertEquals(expected, result);
	}

	@Test
	public void evaluateStringArrayIndex() {
		Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("first"),
				new StringLiteral("second"),
				new StringLiteral("third")
		});
		ArrayIndex operator = new ArrayIndex(array, new NumberLiteral(0));

		StringLiteral expected = new StringLiteral("first");
		Node result = operator.evaluate();

		assertEquals(expected, result);
	}

	@Test
	public void evaluateArrayIndexOutofBound() {
		Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("first"),
				new StringLiteral("second"),
				new StringLiteral("third")
		});
		ArrayIndex operator = new ArrayIndex(array, new NumberLiteral(3));

		Node result = operator.evaluate();

		assertEquals(operator, result);
	}
}
