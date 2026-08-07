package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReverseTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Reverse op = new Reverse(array);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateNotAnArray() {
        Node array = new NumberLiteral(1);

        Reverse op = new Reverse(array);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateReverseNumbers() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3),
                new NumberLiteral(4)
        });

        Reverse op = new Reverse(array);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(TypeEnum.NUMBER, resultArray.getElementType());
        assertEquals(4, resultArray.getValue().length);

        assertEquals(4, resultArray.getValue()[0].getValue());
        assertEquals(3, resultArray.getValue()[1].getValue());
        assertEquals(2, resultArray.getValue()[2].getValue());
        assertEquals(1, resultArray.getValue()[3].getValue());
    }

    @Test
    public void evaluateReverseStrings() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("TWO"),
                new StringLiteral("THREE")
        });

        Reverse op = new Reverse(array);

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(3, resultArray.getValue().length);

        assertEquals("THREE", resultArray.getValue()[0].getValue());
        assertEquals("TWO", resultArray.getValue()[1].getValue());
        assertEquals("ONE", resultArray.getValue()[2].getValue());
    }

    @Test
    public void evaluateReverseEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Reverse op = new Reverse(array);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(0, resultArray.getValue().length);
        assertEquals(TypeEnum.NUMBER, resultArray.getElementType());
    }

    @Test
    public void evaluateReverseSingleElement() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Reverse op = new Reverse(array);

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(1, resultArray.getValue().length);
        assertEquals(1, resultArray.getValue()[0].getValue());
    }

    @Test
    public void evaluateReverseDoesNotMutateOriginalArray() {
        ArrayValues<?> array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Reverse op = new Reverse(array);

        Node result = op.evaluate();

        assertEquals(1, array.getValue()[0].getValue());
        assertEquals(2, array.getValue()[1].getValue());
        assertEquals(3, array.getValue()[2].getValue());

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(3, resultArray.getValue()[0].getValue());
        assertEquals(2, resultArray.getValue()[1].getValue());
        assertEquals(1, resultArray.getValue()[2].getValue());
    }

    @Test
    public void evaluateReverseNestedArrays() {
        Node array = new ArrayValues<>(new ArrayType(TypeEnum.NUMBER), new ArrayValues[]{
                new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                        new NumberLiteral(1)
                }),
                new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                        new NumberLiteral(2)
                })
        });

        Reverse op = new Reverse(array);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(2, resultArray.getValue().length);

        ArrayValues<?> first = (ArrayValues<?>) resultArray.getValue()[0];
        ArrayValues<?> second = (ArrayValues<?>) resultArray.getValue()[1];

        assertEquals(2, first.getValue()[0].getValue());
        assertEquals(1, second.getValue()[0].getValue());
    }

    @Test
    public void evaluateUnevaluatableArray() {
        Node array = new Variable("unknown");

        Reverse op = new Reverse(array);

        Node result = op.evaluate();

        assertSame(op, result);
    }

    @Test
    public void evaluatePreservesElementType() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("A"),
                new StringLiteral("B")
        });

        Reverse op = new Reverse(array);

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(TypeEnum.STRING, resultArray.getElementType());
    }
}
