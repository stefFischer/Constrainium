package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.ForAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class OneOfStringTest {
	@Test
	public void evaluateTrueOneOptionTest() {
		Node value = new StringLiteral("ONE");
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		OneOfString operator = new OneOfString(value, numberOfOptions);
		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrueOnlySameOptionsTest() {
		Variable value = new Variable("a");
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		OneOfString operator = new OneOfString(value, numberOfOptions);

		testValues(operator, value, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("THREE"),
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("THREE"),
				new StringLiteral("TWO"),
				new StringLiteral("ONE")
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
		OneOfString operator = new OneOfString(value, numberOfOptions);

		testValues(operator, value, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("THREE"),
				new StringLiteral("FOUR")
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
		OneOfString operator = new OneOfString(value, numberOfOptions);

		testValues(operator, value, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("THREE"),
				new StringLiteral("FOUR"),
				new StringLiteral("THREE")
		}, new boolean[]{
				true,
				true,
				true,
				false,
				true
		});
	}

	private void testValues(OneOfString operator, Variable variable, StringLiteral[] values, boolean[] expected){
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

	@SuppressWarnings("unchecked")
	@Test
	public void evaluateOneOfStringArrayElement(){
		Variable value = new Variable("a");
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		ForAll operator = new ForAll(value, new OneOfString(new Variable(ArrayQuantifier.ELEMENT_NAME), numberOfOptions));

		testArrayValues(operator, value, new ArrayValues[]{
        	ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("ONE"), new StringLiteral("TWO"))),
			ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("THREE"), new StringLiteral("TWO"))),
			ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("ONE"), new StringLiteral("THREE"))),
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
	public void evaluateOneOfStringArrayElementWithViolations(){
		Variable value = new Variable("a");
		NumberLiteral numberOfOptions = new NumberLiteral(3);
		ForAll operator = new ForAll(value, new OneOfString(new Variable(ArrayQuantifier.ELEMENT_NAME), numberOfOptions));

		testArrayValues(operator, value, new ArrayValues[]{
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("ONE"), new StringLiteral("TWO"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("THREE"), new StringLiteral("TWO"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("ONE"), new StringLiteral("THREE"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("ONE"), new StringLiteral("FOUR"))),
				ArrayValues.createArrayValuesFromList(List.of(new StringLiteral("TWO"), new StringLiteral("THREE")))
		}, new boolean[]{
				true,
				true,
				true,
				false,
				true
		});
	}

	private void testArrayValues(ForAll operator, Variable variable, ArrayValues<StringLiteral>[] values, boolean[] expected){
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
