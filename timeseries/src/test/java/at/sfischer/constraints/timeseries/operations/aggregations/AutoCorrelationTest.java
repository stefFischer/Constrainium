package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AutoCorrelationTest {

    @Test
    void shouldCalculateLagZeroAsOne() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3),
                                new NumberLiteral(4)
                        }
                ),
                new IntegerLiteral(0)
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(1.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldCalculatePositiveLagOneAutoCorrelation() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3),
                                new NumberLiteral(4),
                                new NumberLiteral(5)
                        }
                ),
                new IntegerLiteral(1)
        );

        Node result = operation.evaluate();

        // x = [1, 2, 3, 4, 5]
        // mean = 3
        // numerator = (-2 * -1) + (-1 * 0) + (0 * 1) + (1 * 2) = 4
        // denominator = 4 + 1 + 0 + 1 + 4 = 10
        // AutoCorrelation = 0.4
        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.4, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldCalculateNegativeLagOneAutoCorrelation() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(-1),
                                new NumberLiteral(1),
                                new NumberLiteral(-1),
                                new NumberLiteral(1)
                        }
                ),
                new IntegerLiteral(1)
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(-0.8, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldCalculateLagTwoAutoCorrelation() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3),
                                new NumberLiteral(4),
                                new NumberLiteral(5)
                        }
                ),
                new IntegerLiteral(2)
        );

        Node result = operation.evaluate();

        // mean = 3
        // numerator = (-2 * 0) + (-1 * 1) + (0 * 2) = -1
        // denominator = 10
        // AutoCorrelation = -0.1
        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(-0.1, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldReturnZeroForConstantSeries() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(5),
                                new NumberLiteral(5),
                                new NumberLiteral(5),
                                new NumberLiteral(5)
                        }
                ),
                new IntegerLiteral(1)
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldWorkWithIntegerValues() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.INTEGER,
                        new Value<?>[]{
                                new IntegerLiteral(1),
                                new IntegerLiteral(2),
                                new IntegerLiteral(3),
                                new IntegerLiteral(4),
                                new IntegerLiteral(5)
                        }
                ),
                new IntegerLiteral(1)
        );

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.4, ((NumberLiteral) result).getValue().doubleValue(), 1e-9);
    }

    @Test
    void shouldReturnThisForEmptyArray() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[0]
                ),
                new IntegerLiteral(1)
        );

        assertSame(operation, operation.evaluate());
    }

    @Test
    void shouldReturnThisWhenLagIsEqualToArrayLength() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3)
                        }
                ),
                new IntegerLiteral(3)
        );

        assertSame(operation, operation.evaluate());
    }

    @Test
    void shouldReturnThisWhenLagIsGreaterThanArrayLength() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3)
                        }
                ),
                new IntegerLiteral(4)
        );

        assertSame(operation, operation.evaluate());
    }

    @Test
    void shouldReturnThisForNegativeLag() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3)
                        }
                ),
                new IntegerLiteral(-1)
        );

        assertSame(operation, operation.evaluate());
    }

    @Test
    void shouldReturnThisForNonNumericArray() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.STRING,
                        new Value<?>[]{
                                new StringLiteral("a"),
                                new StringLiteral("b"),
                                new StringLiteral("c")
                        }
                ),
                new IntegerLiteral(1)
        );

        assertSame(operation, operation.evaluate());
    }

    @Test
    void shouldReturnThisWhenArrayCannotBeEvaluated() {
        Variable array = new Variable("array");
        AutoCorrelation operation = new AutoCorrelation(
                array,
                new IntegerLiteral(1)
        );

        assertSame(operation, operation.evaluate());
    }

    @Test
    void shouldReturnThisWhenLagCannotBeEvaluated() {
        Variable lag = new Variable("lag");
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3)
                        }
                ),
                lag
        );

        assertSame(operation, operation.evaluate());
    }

    @Test
    void shouldValidateNumericArrayAndIntegerLag() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3)
                        }
                ),
                new IntegerLiteral(1)
        );

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    void shouldValidateIntegerArray() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.INTEGER,
                        new Value<?>[]{
                                new IntegerLiteral(1),
                                new IntegerLiteral(2),
                                new IntegerLiteral(3)
                        }
                ),
                new IntegerLiteral(1)
        );

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    void shouldRejectNonNumericArray() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.STRING,
                        new Value<?>[]{
                                new StringLiteral("a"),
                                new StringLiteral("b")
                        }
                ),
                new IntegerLiteral(1)
        );

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertFalse(context.isValid());
    }

    @Test
    void shouldRejectNonIntegerLag() {
        AutoCorrelation operation = new AutoCorrelation(
                new ArrayValues<>(
                        TypeEnum.NUMBER,
                        new Value<?>[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3)
                        }
                ),
                new NumberLiteral(1.5)
        );

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertFalse(context.isValid());
    }
}