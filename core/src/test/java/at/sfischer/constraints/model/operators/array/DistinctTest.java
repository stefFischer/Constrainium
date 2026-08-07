package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DistinctTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Distinct op = new Distinct(array);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateNotAnArray() {
        Distinct op = new Distinct(new NumberLiteral(1));

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateNoDuplicates() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Distinct op = new Distinct(array);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> values = (ArrayValues<?>) result;

        assertEquals(TypeEnum.NUMBER, values.getElementType());
        assertEquals(3, values.getValue().length);

        assertEquals(1, values.getValue()[0].getValue());
        assertEquals(2, values.getValue()[1].getValue());
        assertEquals(3, values.getValue()[2].getValue());
    }

    @Test
    public void evaluateRemovesDuplicates() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(1),
                new NumberLiteral(3),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Distinct op = new Distinct(array);

        Node result = op.evaluate();

        ArrayValues<?> values = (ArrayValues<?>) result;

        assertEquals(3, values.getValue().length);

        assertEquals(1, values.getValue()[0].getValue());
        assertEquals(2, values.getValue()[1].getValue());
        assertEquals(3, values.getValue()[2].getValue());
    }

    @Test
    public void evaluatePreservesOrder() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("THREE"),
                new StringLiteral("ONE"),
                new StringLiteral("THREE"),
                new StringLiteral("TWO"),
                new StringLiteral("ONE")
        });

        Distinct op = new Distinct(array);

        Node result = op.evaluate();

        ArrayValues<?> values = (ArrayValues<?>) result;

        assertEquals(3, values.getValue().length);

        assertEquals("THREE", values.getValue()[0].getValue());
        assertEquals("ONE", values.getValue()[1].getValue());
        assertEquals("TWO", values.getValue()[2].getValue());
    }

    @Test
    public void evaluateAllDuplicates() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(5),
                new NumberLiteral(5),
                new NumberLiteral(5),
                new NumberLiteral(5)
        });

        Distinct op = new Distinct(array);

        Node result = op.evaluate();

        ArrayValues<?> values = (ArrayValues<?>) result;

        assertEquals(1, values.getValue().length);
        assertEquals(5, values.getValue()[0].getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Distinct op = new Distinct(array);

        Node result = op.evaluate();

        ArrayValues<?> values = (ArrayValues<?>) result;

        assertEquals(TypeEnum.NUMBER, values.getElementType());
        assertEquals(0, values.getValue().length);
    }

    @Test
    public void evaluateStringArray() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("TWO"),
                new StringLiteral("ONE")
        });

        Distinct op = new Distinct(array);

        Node result = op.evaluate();

        ArrayValues<?> values = (ArrayValues<?>) result;

        assertEquals(TypeEnum.STRING, values.getElementType());
        assertEquals(2, values.getValue().length);

        assertEquals("ONE", values.getValue()[0].getValue());
        assertEquals("TWO", values.getValue()[1].getValue());
    }

    @Test
    public void evaluateOriginalArrayUnchanged() {
        ArrayValues<?> array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(1)
        });

        Distinct op = new Distinct(array);

        op.evaluate();

        assertEquals(3, array.getValue().length);
        assertEquals(1, array.getValue()[0].getValue());
        assertEquals(2, array.getValue()[1].getValue());
        assertEquals(1, array.getValue()[2].getValue());
    }

    @Test
    public void evaluateUnevaluatableArray() {
        Distinct op = new Distinct(new Variable("array"));

        Node result = op.evaluate();

        assertSame(op, result);
    }

    @Test
    public void evaluateNestedArrays() {
        ArrayValues<?> inner1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        ArrayValues<?> inner2 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2)
        });

        Node array = new ArrayValues<>(new ArrayType(TypeEnum.NUMBER), new ArrayValues[]{
                inner1,
                inner2,
                inner1,
                inner2
        });

        Distinct op = new Distinct(array);

        Node result = op.evaluate();

        ArrayValues<?> values = (ArrayValues<?>) result;

        assertEquals(2, values.getValue().length);
        assertSame(inner1, values.getValue()[0]);
        assertSame(inner2, values.getValue()[1]);
    }

    @Test
    public void evaluatePreservesElementType() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("A"),
                new StringLiteral("A")
        });

        Distinct op = new Distinct(array);

        Node result = op.evaluate();

        ArrayValues<?> values = (ArrayValues<?>) result;

        assertEquals(TypeEnum.STRING, values.getElementType());
    }
}
