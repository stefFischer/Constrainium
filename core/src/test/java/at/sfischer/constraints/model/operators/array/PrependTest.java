package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.AdditionOperator;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PrependTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Prepend op = new Prepend(array, new NumberLiteral(3));

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateStringPrepend() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE")
        });

        Prepend op = new Prepend(array, new StringLiteral("TWO"));

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateTypeMismatch() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Prepend op = new Prepend(array, new StringLiteral("STRING"));

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(2, context.getMessages().size());
    }

    @Test
    public void validateNotAnArray() {
        Node array = new NumberLiteral(1);

        Prepend op = new Prepend(array, new NumberLiteral(2));

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateNullValue() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Prepend op = new Prepend(array, null);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluatePrependNumber() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Prepend op = new Prepend(array, new NumberLiteral(3));

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(TypeEnum.NUMBER, resultArray.getElementType());
        assertEquals(3, resultArray.getValue().length);

        assertEquals(3, resultArray.getValue()[0].getValue());
        assertEquals(1, resultArray.getValue()[1].getValue());
        assertEquals(2, resultArray.getValue()[2].getValue());
    }

    @Test
    public void evaluatePrependString() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("TWO")
        });

        Prepend op = new Prepend(array, new StringLiteral("THREE"));

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(3, resultArray.getValue().length);

        assertEquals("THREE", resultArray.getValue()[0].getValue());
        assertEquals("ONE", resultArray.getValue()[1].getValue());
        assertEquals("TWO", resultArray.getValue()[2].getValue());
    }

    @Test
    public void evaluatePrependEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Prepend op = new Prepend(array, new NumberLiteral(1));

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(1, resultArray.getValue().length);
        assertEquals(1, resultArray.getValue()[0].getValue());
    }

    @Test
    public void evaluateOriginalArrayIsUnchanged() {
        ArrayValues<NumberLiteral> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new NumberLiteral[]{
                        new NumberLiteral(1),
                        new NumberLiteral(2)
                }
        );

        Prepend op = new Prepend(array, new NumberLiteral(3));

        Node result = op.evaluate();

        assertEquals(2, array.getValue().length);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(3, resultArray.getValue().length);
    }

    @Test
    public void evaluateUnevaluatableValue() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Prepend op = new Prepend(array, new Variable("unknown"));

        Node result = op.evaluate();

        assertSame(op, result);
    }

    @Test
    public void evaluateValueExpression() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Node value = new AdditionOperator(
                new NumberLiteral(2),
                new NumberLiteral(3)
        );

        Prepend op = new Prepend(array, value);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(2, resultArray.getValue().length);
        assertEquals(5.0, resultArray.getValue()[0].getValue());
    }

    @Test
    public void evaluatePrependNestedArrayValue() {
        Node array = new ArrayValues<>(new ArrayType(TypeEnum.NUMBER), new ArrayValues[]{
                new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                        new NumberLiteral(1)
                })
        });

        Node value = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2)
        });

        Prepend op = new Prepend(array, value);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(2, resultArray.getValue().length);
    }
}