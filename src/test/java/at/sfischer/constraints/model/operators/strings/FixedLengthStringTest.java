package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.OneOf;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class FixedLengthStringTest {
	@Test
	public void evaluate() {
		OneOf operator = new OneOf(new StringLength(new StringLiteral("ONE")), new NumberLiteral(1));
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseTooManyOptionsTest() {
		Variable value = new Variable("a");
		OneOf operator = new OneOf(new StringLength(value), new NumberLiteral(1));

		testValues(operator, value, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("THREE"),
		}, new boolean[]{
				true,
				true,
				false
		});
	}

	private void testValues(OneOf operator, Variable variable, StringLiteral[] values, boolean[] expected){
		if(values.length != expected.length){
			throw new IllegalArgumentException("Values and expected outcomes need to have the same length.");
		}

		for (int i = 0; i < values.length; i++) {
			Map<Variable, Literal<?>> valuesMap = new HashMap<>();
			valuesMap.put(variable, values[i]);
			Node node = operator.setVariableValues(valuesMap);
			Node result = node.evaluate();
			assertInstanceOf(BooleanLiteral.class,result);
			assertEquals(expected[i], ((BooleanLiteral)result).getValue());
		}
	}
}
