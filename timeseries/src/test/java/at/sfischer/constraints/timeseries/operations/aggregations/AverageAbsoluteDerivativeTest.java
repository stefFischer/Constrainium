package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AverageAbsoluteDerivativeTest {

    @Test
    void shouldCalculateAverageAbsoluteDerivative() {
        ArrayValues<?> array = new ArrayValues<>(
            TypeEnum.INTEGER,
            new Value<?>[] {
                new IntegerLiteral(2),
                new IntegerLiteral(5),
                new IntegerLiteral(3),
                new IntegerLiteral(7)
            }
        );

        AverageAbsoluteDerivative operation = new AverageAbsoluteDerivative(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(3.0, ((NumberLiteral) result).getValue());
    }

    @Test
    void shouldHandleNegativeDifferences() {
        ArrayValues<?> array = new ArrayValues<>(
            TypeEnum.INTEGER,
            new Value<?>[] {
                    new IntegerLiteral(10),
                    new IntegerLiteral(5),
                    new IntegerLiteral(2)
            }
        );

        AverageAbsoluteDerivative operation = new AverageAbsoluteDerivative(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(4.0, ((NumberLiteral) result).getValue());
    }

    @Test
    void shouldHandleMixedPositiveAndNegativeDifferences() {
        ArrayValues<?> array = new ArrayValues<>(
            TypeEnum.INTEGER,
            new Value<?>[] {
                    new IntegerLiteral(1),
                    new IntegerLiteral(4),
                    new IntegerLiteral(2),
                    new IntegerLiteral(8)
            }
        );

        AverageAbsoluteDerivative operation =
                new AverageAbsoluteDerivative(array);

        Node result = operation.evaluate();

        // (|4 - 1| + |2 - 4| + |8 - 2|) / 3
        // = (3 + 2 + 6) / 3
        // = 11 / 3
        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(11.0 / 3.0, ((NumberLiteral) result).getValue());
    }

    @Test
    void shouldReturnZeroForConstantArray() {
        ArrayValues<?> array = new ArrayValues<>(
            TypeEnum.INTEGER,
            new Value<?>[] {
                    new IntegerLiteral(5),
                    new IntegerLiteral(5),
                    new IntegerLiteral(5),
                    new IntegerLiteral(5)
            }
        );

        AverageAbsoluteDerivative operation =
                new AverageAbsoluteDerivative(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue());
    }

    @Test
    void shouldReturnZeroForSingleElementArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(5)
                });

        AverageAbsoluteDerivative operation = new AverageAbsoluteDerivative(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0, ((NumberLiteral) result).getValue());
    }

    @Test
    void shouldReturnZeroForEmptyArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {});

        AverageAbsoluteDerivative operation = new AverageAbsoluteDerivative(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0, ((NumberLiteral) result).getValue());
    }

    @Test
    void shouldSupportNumberValues() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1.5),
                        new NumberLiteral(4.0),
                        new NumberLiteral(2.0)
                });

        AverageAbsoluteDerivative operation = new AverageAbsoluteDerivative(array);

        Node result = operation.evaluate();

        // (2.5 + 2.0) / 2
        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(2.25, ((NumberLiteral) result).getValue());
    }

    @Test
    void shouldReturnThisWhenArrayCannotBeEvaluated() {
        AverageAbsoluteDerivative operation = new AverageAbsoluteDerivative(new Variable("array"));

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisWhenArrayContainsNonNumericValue() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.ANY,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new StringLiteral("invalid"),
                        new IntegerLiteral(3)
                });

        AverageAbsoluteDerivative operation = new AverageAbsoluteDerivative(array);

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldValidateNumericIntegerArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                });

        AverageAbsoluteDerivative operation = new AverageAbsoluteDerivative(array);

        ValidationContext context = new ValidationContext();

        operation.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    void shouldValidateNumericNumberArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1.0),
                        new NumberLiteral(2.0)
                });

        AverageAbsoluteDerivative operation = new AverageAbsoluteDerivative(array);

        ValidationContext context = new ValidationContext();

        operation.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    void shouldRejectNonNumericArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.STRING,
                new Value<?>[] {
                        new StringLiteral("a"),
                        new StringLiteral("b")
                });

        AverageAbsoluteDerivative operation = new AverageAbsoluteDerivative(array);

        ValidationContext context = new ValidationContext();

        operation.validate(context);

        assertFalse(context.isValid());
    }

    @Test
    void shouldHaveCorrectChildren() {
        Node array = new Variable("array");

        AverageAbsoluteDerivative operation = new AverageAbsoluteDerivative(array);

        assertEquals(
                List.of(array),
                operation.getChildren()
        );
    }

    @Test
    void shouldCloneOperation() {
        AverageAbsoluteDerivative operation = new AverageAbsoluteDerivative(new Variable("array"));

        Node clone = operation.cloneNode();

        assertInstanceOf(AverageAbsoluteDerivative.class, clone);
        assertNotSame(operation, clone);
        assertEquals(operation, clone);
    }
}