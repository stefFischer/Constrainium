package at.sfischer.constraints.timeseries.operations.transformations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AddGaussianNoiseTest {

    @Test
    void shouldAddNoiseToArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2),
                        new IntegerLiteral(3)
                });

        AddGaussianNoise operation = new AddGaussianNoise(
                array,
                new NumberLiteral(1.0)
        );

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(TypeEnum.NUMBER, resultArray.getElementType());
        assertEquals(3, resultArray.getValue().length);
        for (Value<?> value : resultArray.getValue()) {
            assertInstanceOf(NumberLiteral.class, value);
        }
    }

    @Test
    void shouldReturnOriginalValuesWhenStandardDeviationIsZero() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2),
                        new IntegerLiteral(3)
                });

        AddGaussianNoise operation = new AddGaussianNoise(
                array,
                new NumberLiteral(0.0)
        );

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(3, resultArray.getValue().length);
        assertEquals(1.0, ((Number) resultArray.getValue()[0].getValue()).doubleValue());
        assertEquals(2.0, ((Number) resultArray.getValue()[1].getValue()).doubleValue());
        assertEquals(3.0, ((Number) resultArray.getValue()[2].getValue()).doubleValue());
    }

    @Test
    void shouldHandleEmptyArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {});

        AddGaussianNoise operation = new AddGaussianNoise(
                array,
                new NumberLiteral(1.0)
        );

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(0, resultArray.getValue().length);
        assertEquals(TypeEnum.NUMBER, resultArray.getElementType());
    }

    @Test
    void shouldReturnThisWhenArrayIsNotEvaluatable() {
        Variable array = new Variable("array");

        AddGaussianNoise operation = new AddGaussianNoise(
                array,
                new NumberLiteral(1.0)
        );

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisForNegativeStandardDeviation() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                });

        AddGaussianNoise operation = new AddGaussianNoise(
                array,
                new NumberLiteral(-1.0)
        );

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

        AddGaussianNoise operation = new AddGaussianNoise(
                array,
                new NumberLiteral(1.0)
        );

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisForNonNumericStandardDeviation() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                });

        AddGaussianNoise operation = new AddGaussianNoise(
                array,
                new StringLiteral("noise")
        );

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldValidateNumericArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1.0),
                        new NumberLiteral(2.0)
                });

        AddGaussianNoise operation = new AddGaussianNoise(
                array,
                new NumberLiteral(0.5)
        );

        ValidationContext context = new ValidationContext();

        operation.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    void shouldValidateIntegerArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                });

        AddGaussianNoise operation = new AddGaussianNoise(
                array,
                new NumberLiteral(0.5)
        );

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

        AddGaussianNoise operation = new AddGaussianNoise(
                array,
                new NumberLiteral(1.0)
        );

        ValidationContext context = new ValidationContext();

        operation.validate(context);

        assertFalse(context.isValid());
    }

    @Test
    void shouldRejectNonNumericStandardDeviation() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                });

        AddGaussianNoise operation = new AddGaussianNoise(
                array,
                new StringLiteral("noise")
        );

        ValidationContext context = new ValidationContext();

        operation.validate(context);

        assertFalse(context.isValid());
    }
}
