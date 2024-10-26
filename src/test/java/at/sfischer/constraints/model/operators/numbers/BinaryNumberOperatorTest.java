package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BinaryNumberOperatorTest {
	@Test
	public void validate() {
		Node left = new NumberLiteral(2);
		Node right = new NumberLiteral(2);
		BinaryNumberOperator operator = new AdditionOperator(left, right);

		assertTrue(operator.validate());
	}

	@Test
	public void validateLeftNull() {
		Node right = new NumberLiteral(2);
		BinaryNumberOperator operator = new AdditionOperator(null, right);

		assertFalse(operator.validate());
	}

	@Test
	public void validateRightNull() {
		Node left = new NumberLiteral(2);
		BinaryNumberOperator operator = new AdditionOperator(left, null);

		assertFalse(operator.validate());
	}
	@Test
	public void validateLeftNotNumber() {
		Node left = new BooleanLiteral(true);
		Node right = new NumberLiteral(2);
		BinaryNumberOperator operator = new AdditionOperator(left, right);

		assertFalse(operator.validate());
	}

	@Test
	public void validateRightNotNumber() {
		Node left = new NumberLiteral(2);
		Node right = new BooleanLiteral(true);
		BinaryNumberOperator operator = new AdditionOperator(left, right);

		assertFalse(operator.validate());
	}

}
