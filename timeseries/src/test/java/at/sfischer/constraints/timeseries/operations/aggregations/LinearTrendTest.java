package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LinearTrendTest {

    @Test
    void shouldCalculatePositiveLinearTrend() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2),
                        new IntegerLiteral(3),
                        new IntegerLiteral(4),
                        new IntegerLiteral(5)
                }
        );
        LinearTrend operation = new LinearTrend(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(1.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldCalculateNegativeLinearTrend() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(10),
                        new IntegerLiteral(8),
                        new IntegerLiteral(6),
                        new IntegerLiteral(4),
                        new IntegerLiteral(2)
                }
        );
        LinearTrend operation = new LinearTrend(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(-2.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldReturnZeroForConstantSeries() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(5),
                        new IntegerLiteral(5),
                        new IntegerLiteral(5),
                        new IntegerLiteral(5)
                }
        );
        LinearTrend operation = new LinearTrend(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldCalculateTrendForNumberValues() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1.5),
                        new NumberLiteral(3.0),
                        new NumberLiteral(4.5),
                        new NumberLiteral(6.0)
                }
        );
        LinearTrend operation = new LinearTrend(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(1.5, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldBeInvariantToConstantOffset() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(101),
                        new IntegerLiteral(102),
                        new IntegerLiteral(103),
                        new IntegerLiteral(104)
                }
        );
        LinearTrend operation = new LinearTrend(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(1.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldCalculateTrendForNonLinearSeries() {
        // x = [1, 4, 9, 16]
        //
        // The least-squares slope is 5.
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(4),
                        new IntegerLiteral(9),
                        new IntegerLiteral(16)
                }
        );
        LinearTrend operation = new LinearTrend(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(5.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldCalculateTrendForAlternatingValues() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(-1),
                        new IntegerLiteral(1),
                        new IntegerLiteral(-1)
                }
        );
        LinearTrend operation = new LinearTrend(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(-0.4, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldReturnZeroForSingleElement() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(42)
                }
        );
        LinearTrend operation = new LinearTrend(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldReturnZeroForEmptyArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {}
        );
        LinearTrend operation = new LinearTrend(array);

        Node result = operation.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldReturnThisWhenArrayCannotBeEvaluated() {
        LinearTrend operation = new LinearTrend(new Variable("array"));

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
                }
        );
        LinearTrend operation = new LinearTrend(array);

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldValidateIntegerArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2),
                        new IntegerLiteral(3)
                }
        );
        LinearTrend operation = new LinearTrend(array);

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    void shouldValidateNumberArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1.0),
                        new NumberLiteral(2.5),
                        new NumberLiteral(3.0)
                }
        );
        LinearTrend operation = new LinearTrend(array);

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
                }
        );
        LinearTrend operation = new LinearTrend(array);

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertFalse(context.isValid());
    }
}
