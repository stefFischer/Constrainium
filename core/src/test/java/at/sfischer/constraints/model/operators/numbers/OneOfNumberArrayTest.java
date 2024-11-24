package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class OneOfNumberArrayTest {
	@Test
	public void evaluateTrueOneOptionTest() {
		Node value = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(2)
		});
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		OneOfNumberArray operator = new OneOfNumberArray(value, numberOfOptions);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void evaluateTrueOnlySameOptionsTest() {
		Variable value = new Variable("a");
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		OneOfNumberArray operator = new OneOfNumberArray(value, numberOfOptions);

		testValues(operator, value, new ArrayValues[]{
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(1), new NumberLiteral(2)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(3), new NumberLiteral(4)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(1), new NumberLiteral(2)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(5), new NumberLiteral(6)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(1), new NumberLiteral(2)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(3), new NumberLiteral(4)})
		}, new boolean[]{
				true,
				true,
				true,
				true,
				true,
				true
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void evaluateFalseTooManyOptionsTest() {
		Variable value = new Variable("a");
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		OneOfNumberArray operator = new OneOfNumberArray(value, numberOfOptions);

		testValues(operator, value, new ArrayValues[]{
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(1), new NumberLiteral(2)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(3), new NumberLiteral(4)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(1), new NumberLiteral(2)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(5), new NumberLiteral(6)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(1), new NumberLiteral(2)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(8), new NumberLiteral(9)})
		}, new boolean[]{
				true,
				true,
				true,
				true,
				true,
				false
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void evaluateFalseFollowedTrueTest() {
		Variable value = new Variable("a");
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		OneOfNumberArray operator = new OneOfNumberArray(value, numberOfOptions);

		testValues(operator, value, new ArrayValues[]{
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(1), new NumberLiteral(2)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(3), new NumberLiteral(4)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(5), new NumberLiteral(6)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(1), new NumberLiteral(2)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(8), new NumberLiteral(9)}),
				new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{new NumberLiteral(3), new NumberLiteral(4)})
		}, new boolean[]{
				true,
				true,
				true,
				true,
				false,
				true
		});
	}

	private void testValues(OneOfNumberArray operator, Variable variable, ArrayValues<NumberLiteral>[] values, boolean[] expected){
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
