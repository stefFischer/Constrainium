package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.ForAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class IsDateTest {
	@Test
	public void evaluateTrue1() {
		IsDate i = new IsDate(new StringLiteral("2024-01-01"));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrue2() {
		IsDate i = new IsDate(new StringLiteral("2024/01/01"));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalse1() {
		IsDate i = new IsDate(new StringLiteral("01.01.2024"));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalse2() {
		IsDate i = new IsDate(new StringLiteral("2024/13/01"), new ArrayValues<>(TypeEnum.STRING, IsDate.YMD_DATE_PATTERNS));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrue1Pattern2() {
		IsDate i = new IsDate(new StringLiteral("13-01-2024"), new ArrayValues<>(TypeEnum.STRING, IsDate.DMY_DATE_PATTERNS));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrue2Pattern2() {
		IsDate i = new IsDate(new StringLiteral("13/01/2024"), new ArrayValues<>(TypeEnum.STRING, IsDate.DMY_DATE_PATTERNS));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrue1Pattern3() {
		IsDate i = new IsDate(new StringLiteral("01-13-2024"), new ArrayValues<>(TypeEnum.STRING, IsDate.MDY_DATE_PATTERNS));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalse1Pattern3() {
		IsDate i = new IsDate(new StringLiteral("13/01/2024"), new ArrayValues<>(TypeEnum.STRING, IsDate.MDY_DATE_PATTERNS));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTrueSequence1Pattern1() {
		ArrayValues<StringLiteral> value = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
				new StringLiteral("2024/01/13"),
				new StringLiteral("2024-01-13")
		});
		ForAll term = new ForAll(value, new IsDate(new Variable(ArrayQuantifier.ELEMENT_NAME), new ArrayValues<>(TypeEnum.STRING, IsDate.YMD_DATE_PATTERNS)));

		Node result = term.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}
}
