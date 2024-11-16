package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.OneOfNumberArray;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class OneOfStringArrayTest {
	@Test
	public void evaluateTrueOneOptionTest() {
		Node value = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("ONE")
		});
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		OneOfStringArray operator = new OneOfStringArray(value, numberOfOptions);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void evaluateTrueOnlySameOptionsTest() {
		Variable value = new Variable("a");
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		OneOfStringArray operator = new OneOfStringArray(value, numberOfOptions);

		testValues(operator, value, new ArrayValues[]{
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("ONE"), new StringLiteral("TWO"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("THREE"), new StringLiteral("TWO"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("ONE"), new StringLiteral("TWO"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("TWO"), new StringLiteral("THREE")))
		}, new boolean[]{
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
		OneOfStringArray operator = new OneOfStringArray(value, numberOfOptions);

		testValues(operator, value, new ArrayValues[]{
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("ONE"), new StringLiteral("TWO"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("THREE"), new StringLiteral("TWO"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("TWO"), new StringLiteral("THREE"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("ONE"), new StringLiteral("TWO"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("TWO"), new StringLiteral("ONE")))
		}, new boolean[]{
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
		OneOfStringArray operator = new OneOfStringArray(value, numberOfOptions);

		testValues(operator, value, new ArrayValues[]{
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("ONE"), new StringLiteral("TWO"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("THREE"), new StringLiteral("TWO"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("TWO"), new StringLiteral("THREE"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("ONE"), new StringLiteral("TWO"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("TWO"), new StringLiteral("ONE"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("ONE"), new StringLiteral("TWO")))
		}, new boolean[]{
				true,
				true,
				true,
				true,
				false,
				true
		});
	}

	private void testValues(OneOfStringArray operator, Variable variable, ArrayValues<StringLiteral>[] values, boolean[] expected){
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
