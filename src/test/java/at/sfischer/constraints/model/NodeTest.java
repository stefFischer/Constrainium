package at.sfischer.constraints.model;

import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.logic.AndOperator;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.numbers.AdditionOperator;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.model.operators.numbers.LessThanOperator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NodeTest {
	@Test
	public void inferVariableTypesTest1() {
		Node node = new AdditionOperator(new NumberLiteral(4.0), new Variable("x"));
		Map<Variable, Type> expected = new HashMap<>();
		expected.put(new Variable("x"), TypeEnum.NUMBER);

		Map<Variable, Type> actual = node.inferVariableTypes();

		assertEquals(expected, actual);
	}

	@Test
	public void inferVariableTypesTest2() {
		Node node = new AndOperator(new Variable("a"), new LessThanOperator(new Variable("z"), new AdditionOperator(new AdditionOperator(new NumberLiteral(4.0), new Variable("x")), new Variable("y"))));
		Map<Variable, Type> expected = new HashMap<>();
		expected.put(new Variable("a"), TypeEnum.BOOLEAN);
		expected.put(new Variable("x"), TypeEnum.NUMBER);
		expected.put(new Variable("y"), TypeEnum.NUMBER);
		expected.put(new Variable("z"), TypeEnum.NUMBER);

		Map<Variable, Type> actual = node.inferVariableTypes();

		assertEquals(expected, actual);
	}

	@Test
	public void inferVariableTypesTest3() {
		Node node = new ForAll(new Variable("a"), new NotOperator(new EqualOperator(new Variable(ForAll.ELEMENT_NAME), new NumberLiteral(0))));
		Map<Variable, Type> expected = new HashMap<>();
		expected.put(new Variable("a"), new ArrayType(TypeEnum.NUMBER));

		Map<Variable, Type> actual = node.inferVariableTypes();

		assertEquals(expected, actual);
	}

	@Test
	public void inferVariableTypesTestInconsistentTypesError() {
		Node node = new AndOperator(new Variable("a"), new LessThanOperator(new Variable("a"), new NumberLiteral(0)));
		Exception exception = assertThrows(IllegalStateException.class, node::inferVariableTypes);

		String expectedMessage = "Variable Variable{name: a} has inconsistent types: BOOLEAN and NUMBER";
		String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	public void setVariableValuesTest1() {
		Node node = new AdditionOperator(new NumberLiteral(4.0), new Variable("x"));
		Node expected = new AdditionOperator(new NumberLiteral(4.0), new NumberLiteral(2.0));

		Map<Variable, Node> values = new HashMap<>();
		values.put(new Variable("x"), new NumberLiteral(2.0));
		Node actual = node.setVariableValues(values);

		assertThat(actual)
				.usingRecursiveComparison()
				.isEqualTo(expected);
		assertEquals(new NumberLiteral(6.0), actual.evaluate());
	}

	@Test
	public void setVariableValuesTest2() {
		Node node = new AndOperator(new Variable("a"), new LessThanOperator(new Variable("z"), new AdditionOperator(new AdditionOperator(new NumberLiteral(4.0), new Variable("x")), new Variable("y"))));
		Node expected = new AndOperator(new BooleanLiteral(true), new LessThanOperator(new NumberLiteral(3.0), new AdditionOperator(new AdditionOperator(new NumberLiteral(4.0), new NumberLiteral(2.0)), new NumberLiteral(1.0))));

		Map<Variable, Node> values = new HashMap<>();
		values.put(new Variable("a"), new BooleanLiteral(true));
		values.put(new Variable("x"), new NumberLiteral(2.0));
		values.put(new Variable("y"), new NumberLiteral(1.0));
		values.put(new Variable("z"), new NumberLiteral(3.0));
		Node actual = node.setVariableValues(values);

		assertThat(actual)
				.usingRecursiveComparison()
				.isEqualTo(expected);
		assertEquals(new BooleanLiteral(true), actual.evaluate());
	}
}
