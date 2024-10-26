package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class OneOfTest {
	@Test
	public void evaluateTrueOneOptionTest() {
		Node value = new NumberLiteral(2);
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		OneOf operator = new OneOf(value, numberOfOptions);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrueOnlySameOptionsTest() {
		Variable value = new Variable("a");
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		OneOf operator = new OneOf(value, numberOfOptions);

		testValues(operator, value, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(2),
				new NumberLiteral(3),
				new NumberLiteral(1),
				new NumberLiteral(2),
				new NumberLiteral(3),
				new NumberLiteral(2),
				new NumberLiteral(1)
		}, new boolean[]{
				true,
				true,
				true,
				true,
				true,
				true,
				true,
				true
		});
	}

	@Test
	public void evaluateFalseTooManyOptionsTest() {
		Variable value = new Variable("a");
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		OneOf operator = new OneOf(value, numberOfOptions);

		testValues(operator, value, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(2),
				new NumberLiteral(3),
				new NumberLiteral(4)
		}, new boolean[]{
				true,
				true,
				true,
				false
		});
	}

	@Test
	public void evaluateFalseFollowedTrueTest() {
		Variable value = new Variable("a");
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		OneOf operator = new OneOf(value, numberOfOptions);

		testValues(operator, value, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(2),
				new NumberLiteral(3),
				new NumberLiteral(4),
				new NumberLiteral(3)
		}, new boolean[]{
				true,
				true,
				true,
				false,
				true
		});
	}

	private void testValues(OneOf operator, Variable variable, NumberLiteral[] values, boolean[] expected){
		if(values.length != expected.length){
			throw new IllegalArgumentException("Values and expected outcomes need to have the same length.");
		}

		for (int i = 0; i < values.length; i++) {
			Map<Variable, Node> valuesMap = new HashMap<>();
			valuesMap.put(variable, values[i]);
			Node node = operator.setVariableValues(valuesMap);
			Node result = node.evaluate();
			assertInstanceOf(BooleanLiteral.class,result);
			assertEquals(expected[i], ((BooleanLiteral)result).getValue());
		}
	}
}
