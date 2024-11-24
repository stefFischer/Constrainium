package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataValueTest {
	@Test
	public void inconsistentTypeAndValueExceptionTest() {
		Exception exception = assertThrows(IllegalStateException.class, () -> new DataValue<>(TypeEnum.NUMBER, true));

		String expectedMessage = "DataValue type (NUMBER) and value (class java.lang.Boolean) to not match.";
		String actualMessage = exception.getMessage();

		assertEquals(expectedMessage, actualMessage);
	}

	@Test
	public void getLiteralValueNumber() {
		DataValue<Number> dataValue = new DataValue<>(TypeEnum.NUMBER, 2);
		Value<Number> expected = new NumberLiteral(2);
		Value<?> actual = dataValue.getLiteralValue();

		assertEquals(expected, actual);
	}

	@Test
	public void getLiteralValueBoolean() {
		DataValue<Boolean> dataValue = new DataValue<>(TypeEnum.BOOLEAN, true);
		Value<Boolean> expected = BooleanLiteral.TRUE;
		Value<?> actual = dataValue.getLiteralValue();

		assertEquals(expected, actual);
	}

	@Test
	public void getLiteralValueString() {
		DataValue<String> dataValue = new DataValue<>(TypeEnum.STRING, "string");
		Value<String> expected = new StringLiteral("string");
		Value<?> actual = dataValue.getLiteralValue();

		assertEquals(expected, actual);
	}
}
