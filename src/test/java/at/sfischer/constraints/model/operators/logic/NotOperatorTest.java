package at.sfischer.constraints.model.operators.logic;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class NotOperatorTest {
	@Test
	public void evaluateTrue() {
		NotOperator operator = new NotOperator(new BooleanLiteral(false));
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class, result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalse() {
		NotOperator operator = new NotOperator(new BooleanLiteral(true));
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class, result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateDoubleNegation() {
		NotOperator operator = new NotOperator(new NotOperator(new Variable("x")));
		Node result = operator.evaluate();
		Node expected = new Variable("x");

		assertInstanceOf(Variable.class, result);
		assertThat(result)
				.usingRecursiveComparison()
				.isEqualTo(expected);
	}
}
