package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ExistsTest {
	@Test
	public void validate() {
		Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1)
		});
		Node condition = new NotOperator(new EqualOperator(new Variable(ForAll.ELEMENT_NAME), new NumberLiteral(0)));
		Exists f = new Exists(array, condition);
		boolean expected = true;
		boolean actual = f.validate();

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateTrueExistsZeroElement() {
		Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(-1),
				new NumberLiteral(0),
				new NumberLiteral(1)
		});
		Node condition = new EqualOperator(new Variable(ForAll.ELEMENT_NAME), new NumberLiteral(0));
		Exists operator = new Exists(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseExistsZeroElement() {
		Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(-1),
				new NumberLiteral(3),
				new NumberLiteral(1)
		});
		Node condition = new EqualOperator(new Variable(ForAll.ELEMENT_NAME), new NumberLiteral(0));
		Exists operator = new Exists(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrueExistsStringElement() {
		Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("THREE")
		});
		Node condition = new StringEquals(new Variable(ForAll.ELEMENT_NAME), new StringLiteral("TWO"));
		Exists operator = new Exists(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseExistsStringElement() {
		Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("THREE")
		});
		Node condition = new StringEquals(new Variable(ForAll.ELEMENT_NAME), new StringLiteral("TWO"));
		Exists operator = new Exists(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}
}
