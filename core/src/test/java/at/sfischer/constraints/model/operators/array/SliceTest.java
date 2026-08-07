package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SliceTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Slice op = new Slice(
                array,
                new IntegerLiteral(0),
                new IntegerLiteral(2)
        );

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateNotAnArray() {
        Node array = new NumberLiteral(1);

        Slice op = new Slice(
                array,
                new IntegerLiteral(0),
                new IntegerLiteral(1)
        );

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateInvalidStartType() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Slice op = new Slice(
                array,
                new StringLiteral("start"),
                new IntegerLiteral(1)
        );

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(2, context.getMessages().size());
    }

    @Test
    public void validateInvalidEndType() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Slice op = new Slice(
                array,
                new IntegerLiteral(0),
                new StringLiteral("end")
        );

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(2, context.getMessages().size());
    }

    @Test
    public void evaluateNormalSlice() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3),
                new NumberLiteral(4)
        });

        Slice op = new Slice(
                array,
                new IntegerLiteral(1),
                new IntegerLiteral(3)
        );

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(TypeEnum.NUMBER, resultArray.getElementType());
        assertEquals(2, resultArray.getValue().length);

        assertEquals(2, resultArray.getValue()[0].getValue());
        assertEquals(3, resultArray.getValue()[1].getValue());
    }

    @Test
    public void evaluateSliceFromStart() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("TWO"),
                new StringLiteral("THREE")
        });

        Slice op = new Slice(
                array,
                new IntegerLiteral(0),
                new IntegerLiteral(2)
        );

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(2, resultArray.getValue().length);

        assertEquals("ONE", resultArray.getValue()[0].getValue());
        assertEquals("TWO", resultArray.getValue()[1].getValue());
    }

    @Test
    public void evaluateSliceToEnd() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Slice op = new Slice(
                array,
                new IntegerLiteral(1),
                new IntegerLiteral(3)
        );

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(2, resultArray.getValue().length);
        assertEquals(2, resultArray.getValue()[0].getValue());
        assertEquals(3, resultArray.getValue()[1].getValue());
    }

    @Test
    public void evaluateEmptySlice() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Slice op = new Slice(
                array,
                new IntegerLiteral(1),
                new IntegerLiteral(1)
        );

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(0, resultArray.getValue().length);
    }

    @Test
    public void evaluateStartAfterEnd() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Slice op = new Slice(
                array,
                new IntegerLiteral(3),
                new IntegerLiteral(1)
        );

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(0, resultArray.getValue().length);
    }

    @Test
    public void evaluateEndBeyondArrayLength() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Slice op = new Slice(
                array,
                new IntegerLiteral(1),
                new IntegerLiteral(100)
        );

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(2, resultArray.getValue().length);
        assertEquals(2, resultArray.getValue()[0].getValue());
        assertEquals(3, resultArray.getValue()[1].getValue());
    }

    @Test
    public void evaluateNegativeStartIndex() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3),
                new NumberLiteral(4)
        });

        Slice op = new Slice(
                array,
                new IntegerLiteral(-2),
                new IntegerLiteral(4)
        );

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(2, resultArray.getValue().length);
        assertEquals(3, resultArray.getValue()[0].getValue());
        assertEquals(4, resultArray.getValue()[1].getValue());
    }

    @Test
    public void evaluateNegativeEndIndex() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3),
                new NumberLiteral(4)
        });

        Slice op = new Slice(
                array,
                new IntegerLiteral(0),
                new IntegerLiteral(-1)
        );

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(3, resultArray.getValue().length);
        assertEquals(1, resultArray.getValue()[0].getValue());
        assertEquals(3, resultArray.getValue()[2].getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Slice op = new Slice(
                array,
                new IntegerLiteral(0),
                new IntegerLiteral(10)
        );

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(0, resultArray.getValue().length);
    }

    @Test
    public void evaluateUnevaluatableStart() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Slice op = new Slice(
                array,
                new Variable("unknown"),
                new IntegerLiteral(1)
        );

        Node result = op.evaluate();

        assertSame(op, result);
    }

    @Test
    public void evaluatePreservesElementType() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("TWO")
        });

        Slice op = new Slice(
                array,
                new IntegerLiteral(0),
                new IntegerLiteral(1)
        );

        Node result = op.evaluate();

        ArrayValues<?> resultArray = (ArrayValues<?>) result;

        assertEquals(TypeEnum.STRING, resultArray.getElementType());
    }
}
