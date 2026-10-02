package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PearsonKurtosisTest {

    @Test
    void shouldCalculateKnownKurtosis() {
        PearsonKurtosis operation = new PearsonKurtosis(
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

        /*
         * mean = 3
         *
         * second central moment:
         * ((-2)^2 + (-1)^2 + 0^2 + 1^2 + 2^2) / 5
         * = 2
         *
         * fourth central moment:
         * ((-2)^4 + (-1)^4 + 0^4 + 1^4 + 2^4) / 5
         * = 6.8
         *
         * kurtosis = 6.8 / 2^2
         * = 1.7
         */
        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(1.7, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldCalculateKnownHeavyTailedDistribution() {
        PearsonKurtosis operation = new PearsonKurtosis(
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
         * mean = 3.4
         *
         * Pearson kurtosis ≈ 3.1869880277
         */
        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(3.1869880277252673, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldCalculateKurtosisForAlternatingValues() {
        PearsonKurtosis operation = new PearsonKurtosis(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(-1),
                                new NumberLiteral(1),
                                new NumberLiteral(-1),
                                new NumberLiteral(1)
                        }
                )
        );

        Node result = operation.evaluate();

        /*
         * mean = 0.2
         * Pearson kurtosis = 7 / 6 ≈ 1.1666666667
         */
        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(1.1666666666666667, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldReturnZeroForConstantValues() {
        PearsonKurtosis operation = new PearsonKurtosis(
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
        PearsonKurtosis operation = new PearsonKurtosis(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(5)
                        }
                )
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue().doubleValue(),1e-9);
    }

    @Test
    void shouldReturnZeroForEmptyArray() {
        PearsonKurtosis operation = new PearsonKurtosis(
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
        PearsonKurtosis operation = new PearsonKurtosis(
                new ArrayValues<>(
                        TypeEnum.INTEGER,
                        new Value<?>[]{
                                new IntegerLiteral(1),
                                new IntegerLiteral(2),
                                new IntegerLiteral(3),
                                new IntegerLiteral(4),
                                new IntegerLiteral(5)
                        }
                )
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(1.7, ((NumberLiteral) result).getValue().doubleValue(),1e-9);
    }

    @Test
    void shouldBeInvariantToLocationShift() {
        PearsonKurtosis original = new PearsonKurtosis(
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
        PearsonKurtosis shifted = new PearsonKurtosis(
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
        PearsonKurtosis original = new PearsonKurtosis(
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
        PearsonKurtosis scaled = new PearsonKurtosis(
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
    void shouldReturnThisForNonNumericArray() {
        PearsonKurtosis operation = new PearsonKurtosis(
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
        PearsonKurtosis operation = new PearsonKurtosis(
                new Variable("array")
        );

        assertSame(operation, operation.evaluate());
    }

    @Test
    void shouldValidateNumericArray() {
        PearsonKurtosis operation = new PearsonKurtosis(
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
        PearsonKurtosis operation = new PearsonKurtosis(
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
        PearsonKurtosis operation = new PearsonKurtosis(
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