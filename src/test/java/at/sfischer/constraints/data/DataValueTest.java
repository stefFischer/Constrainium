package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

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
		Literal<Number> expected = new NumberLiteral(2);
		Literal<?> actual = dataValue.getLiteralValue();

		assertEquals(expected, actual);
	}

	@Test
	public void getLiteralValueBoolean() {
		DataValue<Boolean> dataValue = new DataValue<>(TypeEnum.BOOLEAN, true);
		Literal<Boolean> expected = new BooleanLiteral(true);
		Literal<?> actual = dataValue.getLiteralValue();

		assertEquals(expected, actual);
	}

	@Test
	public void getLiteralValueString() {
		DataValue<String> dataValue = new DataValue<>(TypeEnum.STRING, "string");
		Literal<String> expected = new StringLiteral("string");
		Literal<?> actual = dataValue.getLiteralValue();

		assertEquals(expected, actual);
	}
}
