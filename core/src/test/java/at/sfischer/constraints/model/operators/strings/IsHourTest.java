package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class IsHourTest {
	@Test
	public void evaluateTruePatter1() {
		IsHour i = new IsHour(new StringLiteral("14:55"), new ArrayValues<>(TypeEnum.STRING, IsHour.HOUR_PATTERNS_24H));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTruePatter1NoLeadingZero() {
		IsHour i = new IsHour(new StringLiteral("6:00"), new ArrayValues<>(TypeEnum.STRING, IsHour.HOUR_PATTERNS_24H));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTruePatter2() {
		IsHour i = new IsHour(new StringLiteral("06:00 PM"), new ArrayValues<>(TypeEnum.STRING, IsHour.HOUR_PATTERNS_12H));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTruePatter2NoLeadingZero() {
		IsHour i = new IsHour(new StringLiteral("6:00 PM"), new ArrayValues<>(TypeEnum.STRING, IsHour.HOUR_PATTERNS_12H));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateFalsePatter2() {
		IsHour i = new IsHour(new StringLiteral("13:66"), new ArrayValues<>(TypeEnum.STRING, IsHour.HOUR_PATTERNS_12H));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(false, ((BooleanLiteral)result).getValue());
	}

	@Test
	public void evaluateTruePatter3() {
		IsHour i = new IsHour(new StringLiteral("14:55:30"), new ArrayValues<>(TypeEnum.STRING, IsHour.HOUR_PATTERNS_24H_S));
		Node result = i.evaluate();

		assertInstanceOf(BooleanLiteral.class,result);
		assertEquals(true, ((BooleanLiteral)result).getValue());
	}
}
