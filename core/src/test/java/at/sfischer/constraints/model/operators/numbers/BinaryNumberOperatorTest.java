package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BinaryNumberOperatorTest {
	@Test
	public void validate() {
		Node left = new NumberLiteral(2);
		Node right = new NumberLiteral(2);
		BinaryNumberOperator operator = new AdditionOperator(left, right);

		ValidationContext context = new ValidationContext();
		operator.validate(context);

		assertTrue(context.isValid());
	}

	@Test
	public void validateLeftNull() {
		Node right = new NumberLiteral(2);
		BinaryNumberOperator operator = new AdditionOperator(null, right);

		ValidationContext context = new ValidationContext();
		operator.validate(context);

		assertFalse(context.isValid());
		assertEquals(1, context.getMessages().size());
	}

	@Test
	public void validateRightNull() {
		Node left = new NumberLiteral(2);
		BinaryNumberOperator operator = new AdditionOperator(left, null);

		ValidationContext context = new ValidationContext();
		operator.validate(context);

		assertFalse(context.isValid());
		assertEquals(1, context.getMessages().size());
	}
	@Test
	public void validateLeftNotNumber() {
		Node left = BooleanLiteral.TRUE;
		Node right = new NumberLiteral(2);
		BinaryNumberOperator operator = new AdditionOperator(left, right);

		ValidationContext context = new ValidationContext();
		operator.validate(context);

		assertFalse(context.isValid());
		assertEquals(1, context.getMessages().size());
	}

	@Test
	public void validateRightNotNumber() {
		Node left = new NumberLiteral(2);
		Node right = BooleanLiteral.TRUE;
		BinaryNumberOperator operator = new AdditionOperator(left, right);

		ValidationContext context = new ValidationContext();
		operator.validate(context);

		assertFalse(context.isValid());
		assertEquals(1, context.getMessages().size());
	}
}
