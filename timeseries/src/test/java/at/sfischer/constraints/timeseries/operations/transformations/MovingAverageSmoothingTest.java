package at.sfischer.constraints.timeseries.operations.transformations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MovingAverageSmoothingTest {

    @Test
    void shouldCalculateMovingAverage() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2),
                        new IntegerLiteral(3),
                        new IntegerLiteral(4),
                        new IntegerLiteral(5)
                });

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(3));

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(TypeEnum.NUMBER, resultArray.getElementType());
        assertEquals(5, resultArray.getValue().length);
        assertEquals(1.0, ((Number) resultArray.getValue()[0].getValue()).doubleValue());
        assertEquals(1.5, ((Number) resultArray.getValue()[1].getValue()).doubleValue());
        assertEquals(2.0, ((Number) resultArray.getValue()[2].getValue()).doubleValue());
        assertEquals(3.0, ((Number) resultArray.getValue()[3].getValue()).doubleValue());
        assertEquals(4.0, ((Number) resultArray.getValue()[4].getValue()).doubleValue());
    }

    @Test
    void shouldUsePartialWindowAtBeginning() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(10),
                        new IntegerLiteral(20),
                        new IntegerLiteral(30)
                });

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(5));

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(10.0, ((Number) resultArray.getValue()[0].getValue()).doubleValue());
        assertEquals(15.0, ((Number) resultArray.getValue()[1].getValue()).doubleValue());
        assertEquals(20.0, ((Number) resultArray.getValue()[2].getValue()).doubleValue());
    }

    @Test
    void shouldUseFullWindowAfterEnoughValues() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2),
                        new IntegerLiteral(4),
                        new IntegerLiteral(8)
                });

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(3));

        Node result = operation.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(1.0, ((Number) resultArray.getValue()[0].getValue()).doubleValue());
        assertEquals(1.5, ((Number) resultArray.getValue()[1].getValue()).doubleValue());
        assertEquals(7.0 / 3.0, ((Number) resultArray.getValue()[2].getValue()).doubleValue());
        assertEquals(14.0 / 3.0, ((Number) resultArray.getValue()[3].getValue()).doubleValue());
    }

    @Test
    void shouldSupportNumberValues() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1.5),
                        new NumberLiteral(2.5),
                        new NumberLiteral(4.0)
                });

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(2));

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(1.5, ((Number) resultArray.getValue()[0].getValue()).doubleValue());
        assertEquals(2.0, ((Number) resultArray.getValue()[1].getValue()).doubleValue());
        assertEquals(3.25, ((Number) resultArray.getValue()[2].getValue()).doubleValue());
    }

    @Test
    void shouldPreserveArrayLength() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2),
                        new IntegerLiteral(3),
                        new IntegerLiteral(4)
                });

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(3));

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(
                array.getValue().length,
                resultArray.getValue().length
        );
    }

    @Test
    void shouldReturnSameValuesForWindowSizeOne() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(5),
                        new IntegerLiteral(3)
                });

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(1));

        Node result = operation.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(1.0, ((Number) resultArray.getValue()[0].getValue()).doubleValue());
        assertEquals(5.0, ((Number) resultArray.getValue()[1].getValue()).doubleValue());
        assertEquals(3.0, ((Number) resultArray.getValue()[2].getValue()).doubleValue());
    }

    @Test
    void shouldHandleWindowLargerThanArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(2),
                        new IntegerLiteral(4),
                        new IntegerLiteral(6)
                });

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(10));

        Node result = operation.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(2.0, ((Number) resultArray.getValue()[0].getValue()).doubleValue());
        assertEquals(3.0, ((Number) resultArray.getValue()[1].getValue()).doubleValue());
        assertEquals(4.0, ((Number) resultArray.getValue()[2].getValue()).doubleValue());
    }

    @Test
    void shouldHandleEmptyArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {});

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(3));

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(TypeEnum.NUMBER, resultArray.getElementType());
        assertEquals(0, resultArray.getValue().length);
    }

    @Test
    void shouldReturnThisWhenArrayCannotBeEvaluated() {
        MovingAverageSmoothing operation =
                new MovingAverageSmoothing(
                        new Variable("array"),
                        new IntegerLiteral(3)
                );

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisWhenWindowSizeCannotBeEvaluated() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                });

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new Variable("windowSize"));

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisForZeroWindowSize() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                });

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(0));

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisForNegativeWindowSize() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                });

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(-1));

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

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(2));

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

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(2));

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

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(2));

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

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new IntegerLiteral(2));

        ValidationContext context = new ValidationContext();

        operation.validate(context);

        assertFalse(context.isValid());
    }

    @Test
    void shouldRejectNonIntegerWindowSize() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                });

        MovingAverageSmoothing operation = new MovingAverageSmoothing(array, new NumberLiteral(2.5));

        ValidationContext context = new ValidationContext();

        operation.validate(context);

        assertFalse(context.isValid());
    }

    @Test
    void shouldCloneOperation() {
        MovingAverageSmoothing operation =
                new MovingAverageSmoothing(
                        new Variable("array"),
                        new IntegerLiteral(3)
                );

        Node clone = operation.cloneNode();

        assertInstanceOf(MovingAverageSmoothing.class, clone);
        assertNotSame(operation, clone);
        assertEquals(operation, clone);
    }
}