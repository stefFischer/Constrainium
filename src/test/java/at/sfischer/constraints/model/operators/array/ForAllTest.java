package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.model.operators.numbers.OneOf;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.IsUrl;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.operators.strings.OneOfString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

public class ForAllTest {
	@Test
	public void validate() {
		Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{ 
		  new NumberLiteral(1)
		});
		Node condition = new NotOperator(new EqualOperator(new Variable(ForAll.ELEMENT_NAME), new NumberLiteral(0)));
		ForAll f = new ForAll(array, condition);
		boolean expected = true;
		boolean actual = f.validate();

		assertEquals(expected, actual);
	}

	@Test
	public void evaluateTrueNoZeroElement() {
		Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(-1),
				new NumberLiteral(3),
				new NumberLiteral(1)
		});
		Node condition = new NotOperator(new EqualOperator(new Variable(ForAll.ELEMENT_NAME), new NumberLiteral(0)));
		ForAll operator = new ForAll(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseNoZeroElement() {
		Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(1),
				new NumberLiteral(-1),
				new NumberLiteral(0),
				new NumberLiteral(1)
		});
		Node condition = new NotOperator(new EqualOperator(new Variable(ForAll.ELEMENT_NAME), new NumberLiteral(0)));
		ForAll operator = new ForAll(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrueElementValues() {
		Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(3),
				new NumberLiteral(3)
		});
		Node condition = new EqualOperator(new Variable(ForAll.ELEMENT_NAME), new NumberLiteral(3));
		ForAll operator = new ForAll(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseElementValues() {
		Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
				new NumberLiteral(0),
				new NumberLiteral(0),
				new NumberLiteral(0),
				new NumberLiteral(1)
		});
		Node condition = new EqualOperator(new Variable(ForAll.ELEMENT_NAME), new NumberLiteral(0));
		ForAll operator = new ForAll(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrueElementOneOfString() {
		Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("THREE"),
				new StringLiteral("TWO")
		});
		Node condition = new OneOfString(new Variable(ForAll.ELEMENT_NAME), new NumberLiteral(3));
		ForAll operator = new ForAll(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseElementOneOfString() {
		Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("THREE"),
				new StringLiteral("FOUR")
		});
		Node condition = new OneOfString(new Variable(ForAll.ELEMENT_NAME), new NumberLiteral(3));
		ForAll operator = new ForAll(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrueElementFixedLengthString() {
		Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("TWO")
		});
		Node condition = new OneOf(new StringLength(new Variable(ForAll.ELEMENT_NAME)), new NumberLiteral(1));
		ForAll operator = new ForAll(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseElementFixedLengthString() {
		Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("ONE"),
				new StringLiteral("TWO"),
				new StringLiteral("THREE")
		});
		Node condition = new OneOf(new StringLength(new Variable(ForAll.ELEMENT_NAME)), new NumberLiteral(1));
		ForAll operator = new ForAll(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrueElementIsUrl() {
		Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("https://www.google.at/wiki"),
				new StringLiteral("https://www.wikipedia.org/"),
				new StringLiteral("https://github.com/")
		});
		Node condition = new IsUrl(new Variable(ForAll.ELEMENT_NAME));
		ForAll operator = new ForAll(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalseElementIsUrl() {
		Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("https://www.google.at/wiki"),
				new StringLiteral("https://www.wikipedia.org/"),
				new StringLiteral("not a url")
		});
		Node condition = new IsUrl(new Variable(ForAll.ELEMENT_NAME));
		ForAll operator = new ForAll(array, condition);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateNestedForAll() {
		String jsonData = "{array:[{numbers:[1]},{numbers:[2]},{numbers:[3]}]}";
		DataObject obj = DataObject.parseData(jsonData);

		Node array = obj.getDataValue("array").getLiteralValue();

		Node condition = new NotOperator(new EqualOperator(new Variable(ForAll.ELEMENT_NAME), new NumberLiteral(0)));
		ForAll innerOperator = new ForAll(new Reference(new Variable(ForAll.ELEMENT_NAME), new StringLiteral("numbers")), condition);
		ForAll operator = new ForAll(array, innerOperator);

		Node result = operator.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}
}
