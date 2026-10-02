package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SkewnessTest {

    @Test
    void shouldReturnZeroForSymmetricDistribution() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3),
                                new NumberLiteral(4),
                                new NumberLiteral(5)
                        }
                )
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldReturnPositiveSkewnessForRightSkewedDistribution() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(2),
                                new NumberLiteral(2),
                                new NumberLiteral(10)
                        }
                )
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        double skewness = ((NumberLiteral) result).getValue().doubleValue();
        assertTrue(skewness > 0);
    }

    @Test
    void shouldReturnNegativeSkewnessForLeftSkewedDistribution() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(-10),
                                new NumberLiteral(-2),
                                new NumberLiteral(-2),
                                new NumberLiteral(-2),
                                new NumberLiteral(-1)
                        }
                )
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        double skewness = ((NumberLiteral) result).getValue().doubleValue();
        assertTrue(skewness < 0);
    }

    @Test
    void shouldCalculateKnownPositiveSkewness() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(2),
                                new NumberLiteral(2),
                                new NumberLiteral(10)
                        }
                )
        );

        Node result = operation.evaluate();

        /*
         * mean = 17 / 5 = 3.4
         *
         * second central moment:
         * ((-2.4)^2 + (-1.4)^2 + (-1.4)^2 + (-1.4)^2 + 6.6^2) / 5
         * = 8.24
         *
         * third central moment:
         * ((-2.4)^3 + (-1.4)^3 + (-1.4)^3 + (-1.4)^3 + 6.6^3) / 5
         * = 28.128
         *
         * skewness = 28.128 / 8.24^(3/2)
         * ≈ 1.4472473208
         */
        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(1.4472473207919647, ((NumberLiteral) result).getValue().doubleValue(), 1e-6);
    }

    @Test
    void shouldCalculateKnownNegativeSkewness() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(-10),
                                new NumberLiteral(-2),
                                new NumberLiteral(-2),
                                new NumberLiteral(-2),
                                new NumberLiteral(-1)
                        }
                )
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(-1.4472473207919647, ((NumberLiteral) result).getValue().doubleValue(), 1e-6);
    }

    @Test
    void shouldBeInvariantToLocationShift() {
        Skewness original = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(2),
                                new NumberLiteral(2),
                                new NumberLiteral(10)
                        }
                )
        );
        Skewness shifted = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(101),
                                new NumberLiteral(102),
                                new NumberLiteral(102),
                                new NumberLiteral(102),
                                new NumberLiteral(110)
                        }
                )
        );

        double originalValue = ((NumberLiteral) original.evaluate()).getValue().doubleValue();
        double shiftedValue = ((NumberLiteral) shifted.evaluate()).getValue().doubleValue();

        assertEquals(originalValue, shiftedValue, 1e-9);
    }

    @Test
    void shouldBeInvariantToPositiveScaling() {
        Skewness original = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(2),
                                new NumberLiteral(2),
                                new NumberLiteral(10)
                        }
                )
        );
        Skewness scaled = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(2),
                                new NumberLiteral(4),
                                new NumberLiteral(4),
                                new NumberLiteral(4),
                                new NumberLiteral(20)
                        }
                )
        );

        double originalValue = ((NumberLiteral) original.evaluate()).getValue().doubleValue();
        double scaledValue = ((NumberLiteral) scaled.evaluate()).getValue().doubleValue();

        assertEquals(originalValue, scaledValue, 1e-9);
    }

    @Test
    void shouldReverseSignWhenValuesAreNegated() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(-1),
                                new NumberLiteral(-2),
                                new NumberLiteral(-2),
                                new NumberLiteral(-2),
                                new NumberLiteral(-10)
                        }
                )
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        double skewness = ((NumberLiteral) result).getValue().doubleValue();
        assertEquals(-1.4472473207919647, skewness, 1e-6);
    }

    @Test
    void shouldReturnZeroForConstantValues() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(5),
                                new NumberLiteral(5),
                                new NumberLiteral(5),
                                new NumberLiteral(5)
                        }
                )
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldReturnZeroForSingleValue() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(5)
                        }
                )
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldReturnZeroForEmptyArray() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[0]
                )
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldWorkWithIntegerValues() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.INTEGER,
                        new Value<?>[]{
                                new IntegerLiteral(1),
                                new IntegerLiteral(2),
                                new IntegerLiteral(2),
                                new IntegerLiteral(2),
                                new IntegerLiteral(10)
                        }
                )
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);

        assertEquals(1.4472473207919647, ((NumberLiteral) result).getValue().doubleValue(), 1e-6);
    }

    @Test
    void shouldReturnThisForNonNumericArray() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.STRING,
                        new Value<?>[]{
                                new StringLiteral("a"),
                                new StringLiteral("b"),
                                new StringLiteral("c")
                        }
                )
        );

        assertSame(operation, operation.evaluate());
    }

    @Test
    void shouldReturnThisWhenArrayCannotBeEvaluated() {
        Skewness operation = new Skewness(
                new Variable("array")
        );

        assertSame(operation, operation.evaluate());
    }

    @Test
    void shouldValidateNumericArray() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3)
                        }
                )
        );

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    void shouldValidateIntegerArray() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.INTEGER,
                        new Value<?>[]{
                                new IntegerLiteral(1),
                                new IntegerLiteral(2),
                                new IntegerLiteral(3)
                        }
                )
        );

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    void shouldRejectNonNumericArray() {
        Skewness operation = new Skewness(
                new ArrayValues<>(
                        TypeEnum.STRING,
                        new Value<?>[]{
                                new StringLiteral("a"),
                                new StringLiteral("b"),
                                new StringLiteral("c")
                        }
                )
        );

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertFalse(context.isValid());
    }
}