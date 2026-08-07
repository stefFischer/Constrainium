package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConcatTest {

    @Test
    public void validate() {
        Node array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Node array2 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2)
        });

        Concat op = new Concat(array1, array2);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateStringArrays() {
        Node array1 = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE")
        });

        Node array2 = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("TWO")
        });

        Concat op = new Concat(array1, array2);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateFirstParameterNotArray() {
        Node array1 = new NumberLiteral(1);

        Node array2 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2)
        });

        Concat op = new Concat(array1, array2);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(2, context.getMessages().size());
    }

    @Test
    public void validateSecondParameterNotArray() {
        Node array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Node array2 = new NumberLiteral(2);

        Concat op = new Concat(array1, array2);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(2, context.getMessages().size());
    }

    @Test
    public void validateElementTypeMismatch() {
        Node array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Node array2 = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE")
        });

        Concat op = new Concat(array1, array2);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateConcatNumbers() {
        Node array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Node array2 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(3),
                new NumberLiteral(4)
        });

        Concat op = new Concat(array1, array2);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(TypeEnum.NUMBER, resultArray.getElementType());
        assertEquals(4, resultArray.getValue().length);

        assertEquals(1, resultArray.getValue()[0].getValue());
        assertEquals(2, resultArray.getValue()[1].getValue());
        assertEquals(3, resultArray.getValue()[2].getValue());
        assertEquals(4, resultArray.getValue()[3].getValue());
    }

    @Test
    public void evaluateConcatStrings() {
        Node array1 = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("TWO")
        });

        Node array2 = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("THREE")
        });

        Concat op = new Concat(array1, array2);

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(3, resultArray.getValue().length);

        assertEquals("ONE", resultArray.getValue()[0].getValue());
        assertEquals("TWO", resultArray.getValue()[1].getValue());
        assertEquals("THREE", resultArray.getValue()[2].getValue());
    }

    @Test
    public void evaluateConcatWithEmptyFirstArray() {
        Node array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Node array2 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Concat op = new Concat(array1, array2);

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(2, resultArray.getValue().length);
        assertEquals(1, resultArray.getValue()[0].getValue());
        assertEquals(2, resultArray.getValue()[1].getValue());
    }

    @Test
    public void evaluateConcatWithEmptySecondArray() {
        Node array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Node array2 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Concat op = new Concat(array1, array2);

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(2, resultArray.getValue().length);
        assertEquals(1, resultArray.getValue()[0].getValue());
        assertEquals(2, resultArray.getValue()[1].getValue());
    }

    @Test
    public void evaluateConcatBothEmptyArrays() {
        Node array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});
        Node array2 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Concat op = new Concat(array1, array2);

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(0, resultArray.getValue().length);
        assertEquals(TypeEnum.NUMBER, resultArray.getElementType());
    }

    @Test
    public void evaluateOriginalArraysAreUnchanged() {
        ArrayValues<?> array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        ArrayValues<?> array2 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2)
        });

        Concat op = new Concat(array1, array2);

        Node result = op.evaluate();

        assertEquals(1, array1.getValue().length);
        assertEquals(1, array2.getValue().length);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(2, resultArray.getValue().length);
    }

    @Test
    public void evaluateUnevaluatableFirstArray() {
        Node array1 = new Variable("unknown");

        Node array2 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Concat op = new Concat(array1, array2);

        Node result = op.evaluate();

        assertSame(op, result);
    }

    @Test
    public void evaluateUnevaluatableSecondArray() {
        Node array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Node array2 = new Variable("unknown");

        Concat op = new Concat(array1, array2);

        Node result = op.evaluate();

        assertSame(op, result);
    }

    @Test
    public void evaluateNestedArrays() {
        Node array1 = new ArrayValues<>(new ArrayType(TypeEnum.NUMBER), new ArrayValues[]{
                new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                        new NumberLiteral(1)
                })
        });

        Node array2 = new ArrayValues<>(new ArrayType(TypeEnum.NUMBER), new ArrayValues[]{
                new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                        new NumberLiteral(2)
                })
        });

        Concat op = new Concat(array1, array2);

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(2, resultArray.getValue().length);
    }
}
