package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.numbers.SubtractionOperator;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class SubtractionOperatorTest {
	@Test
	public void simpleEvaluateTest() {
		Node left = new NumberLiteral(4);
		Node right = new NumberLiteral(2);
		SubtractionOperator operator = new SubtractionOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(NumberLiteral.class,result);
		assertEquals(2.0, ((NumberLiteral)result).getValue());
	}

	@Test
	public void evaluateTest() {
		SubtractionOperator operator = new SubtractionOperator(new SubtractionOperator(new NumberLiteral(2), new NumberLiteral(2)), new NumberLiteral(2));
		Node result = operator.evaluate();

		assertInstanceOf(NumberLiteral.class,result);
		assertEquals(-2.0, ((NumberLiteral)result).getValue());
	}

	@Test
	public void evaluateSimplificationTest1() {
		Node left = new Variable("a");
		Node right = new NumberLiteral(0);
		SubtractionOperator operator = new SubtractionOperator(left, right);
		Node result = operator.evaluate();

		assertInstanceOf(Variable.class,result);
		assertEquals(new Variable("a"), result);
	}
}
