package at.sfischer.constraints.model.operators.logic;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class OrOperatorTest {
	@Test
	public void evaluateTrue() {
		OrOperator operator = new OrOperator(new BooleanLiteral(false), new BooleanLiteral(true));
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class, result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalse() {
		OrOperator operator = new OrOperator(new BooleanLiteral(false), new BooleanLiteral(false));
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class, result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateSimplificationTest1() {
		Node left = new Variable("a");
		Node right = new BooleanLiteral(true);
		OrOperator operator = new OrOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(new BooleanLiteral(true), result);
	}

	@Test
	public void evaluateSimplificationTest2() {
		Node left = new BooleanLiteral(true);
		Node right = new Variable("a");
		OrOperator operator = new OrOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(new BooleanLiteral(true), result);
	}

	@Test
	public void evaluateSimplificationTest3() {
		Node left = new Variable("a");
		Node right = new BooleanLiteral(false);
		OrOperator operator = new OrOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(Variable.class,result);
		assertEquals(new Variable("a"), result);
	}

	@Test
	public void evaluateSimplificationTest4() {
		Node left = new BooleanLiteral(false);
		Node right = new Variable("a");
		OrOperator operator = new OrOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(Variable.class,result);
		assertEquals(new Variable("a"), result);
	}
}
