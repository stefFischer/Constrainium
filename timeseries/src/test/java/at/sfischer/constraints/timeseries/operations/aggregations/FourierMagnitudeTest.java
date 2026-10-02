package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FourierMagnitudeTest {

    @Test
    void shouldCalculateFourierMagnitudeForConstantSignal() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1),
                        new NumberLiteral(1),
                        new NumberLiteral(1),
                        new NumberLiteral(1)
                });

        FourierMagnitude operation = new FourierMagnitude(array, new IntegerLiteral(0));

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(4.0, ((NumberLiteral) result).getValue().doubleValue(),1e-10);
    }

    @Test
    void shouldReturnZeroForNonZeroFrequencyOfConstantSignal() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1),
                        new NumberLiteral(1),
                        new NumberLiteral(1),
                        new NumberLiteral(1)
                });

        FourierMagnitude operation = new FourierMagnitude(array, new IntegerLiteral(1));

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue().doubleValue(),1e-10);
    }

    @Test
    void shouldCalculateKnownFourierCoefficient() {
        /*
         * Signal:
         *
         * [1, 0, -1, 0]
         *
         * At frequency k = 1:
         *
         * X_1 = 1 + 0 + (-1)e^(-i*pi) + 0
         *     = 2
         *
         * Therefore |X_1| = 2.
         */
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(0),
                        new IntegerLiteral(-1),
                        new IntegerLiteral(0)
                });

        FourierMagnitude operation = new FourierMagnitude(array, new IntegerLiteral(1));

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(2.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldCalculateMagnitudeIndependentOfPhase() {
        /*
         * [1, 0, -1, 0] and [0, 1, 0, -1]
         * contain the same frequency, only with a phase shift.
         */
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(0),
                        new IntegerLiteral(1),
                        new IntegerLiteral(0),
                        new IntegerLiteral(-1)
                });

        FourierMagnitude operation = new FourierMagnitude(array, new IntegerLiteral(1));

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(2.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldCalculateZeroFrequencyAsSumOfValues() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(2),
                        new IntegerLiteral(3),
                        new IntegerLiteral(5),
                        new IntegerLiteral(7)
                });

        FourierMagnitude operation = new FourierMagnitude(array, new IntegerLiteral(0));

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(17.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldCalculateNyquistFrequency() {
        /*
         * Alternating signal:
         *
         * [1, -1, 1, -1]
         *
         * has all its energy at the Nyquist frequency k = 2.
         */
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(-1),
                        new IntegerLiteral(1),
                        new IntegerLiteral(-1)
                });

        FourierMagnitude operation = new FourierMagnitude(array, new IntegerLiteral(2));

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(4.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldReturnZeroForEmptyArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {});

        FourierMagnitude operation = new FourierMagnitude(array, new IntegerLiteral(0));

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisForNegativeFrequency() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1),
                        new NumberLiteral(2),
                        new NumberLiteral(3)
                });

        FourierMagnitude operation = new FourierMagnitude(array, new IntegerLiteral(-1));

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisForFrequencyOutsideArrayLength() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1),
                        new NumberLiteral(2),
                        new NumberLiteral(3),
                        new NumberLiteral(4)
                });

        FourierMagnitude operation = new FourierMagnitude(array, new IntegerLiteral(4));

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisWhenArrayCannotBeEvaluated() {
        FourierMagnitude operation =
                new FourierMagnitude(
                        new Variable("array"),
                        new IntegerLiteral(1)
                );

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisWhenFrequencyCannotBeEvaluated() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1),
                        new NumberLiteral(2)
                });

        FourierMagnitude operation = new FourierMagnitude(array, new Variable("frequency"));

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisForNonNumericArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.STRING,
                new Value<?>[] {
                        new StringLiteral("a"),
                        new StringLiteral("b")
                });

        FourierMagnitude operation = new FourierMagnitude(array, new IntegerLiteral(1));

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldValidateNumericArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1),
                        new NumberLiteral(2)
                });

        FourierMagnitude operation = new FourierMagnitude(array, new IntegerLiteral(1));

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

        FourierMagnitude operation = new FourierMagnitude(array, new IntegerLiteral(1));

        ValidationContext context = new ValidationContext();

        operation.validate(context);

        assertFalse(context.isValid());
    }
}