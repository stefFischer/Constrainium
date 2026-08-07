package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SortTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(3),
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Sort sort = new Sort(array);

        ValidationContext context = new ValidationContext();
        sort.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateNotAnArray() {
        Sort sort = new Sort(new NumberLiteral(1));

        ValidationContext context = new ValidationContext();
        sort.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateMissingArrayElementReference() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Sort sort = new Sort(
                array,
                new NumberLiteral(5),
                BooleanLiteral.TRUE
        );

        ValidationContext context = new ValidationContext();
        sort.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateInvalidKeyType() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Sort sort = new Sort(
                array,
                BooleanLiteral.TRUE,
                BooleanLiteral.TRUE
        );

        ValidationContext context = new ValidationContext();
        sort.validate(context);

        assertFalse(context.isValid());
    }

    @Test
    public void evaluateSortNumbersAscendingDefault() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(5),
                new NumberLiteral(2),
                new NumberLiteral(3),
                new NumberLiteral(1)
        });

        Sort sort = new Sort(array);

        Node result = sort.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> values = (ArrayValues<?>) result;

        assertEquals(1, values.getValue()[0].getValue());
        assertEquals(2, values.getValue()[1].getValue());
        assertEquals(3, values.getValue()[2].getValue());
        assertEquals(5, values.getValue()[3].getValue());
    }

    @Test
    public void evaluateSortNumbersAscendingExplicit() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(4),
                new NumberLiteral(1),
                new NumberLiteral(3),
                new NumberLiteral(2)
        });

        Sort sort = new Sort(array, BooleanLiteral.TRUE);

        ArrayValues<?> values = (ArrayValues<?>) sort.evaluate();

        assertEquals(1, values.getValue()[0].getValue());
        assertEquals(2, values.getValue()[1].getValue());
        assertEquals(3, values.getValue()[2].getValue());
        assertEquals(4, values.getValue()[3].getValue());
    }

    @Test
    public void evaluateSortNumbersDescending() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(4),
                new NumberLiteral(1),
                new NumberLiteral(3),
                new NumberLiteral(2)
        });

        Sort sort = new Sort(array, BooleanLiteral.FALSE);

        ArrayValues<?> values = (ArrayValues<?>) sort.evaluate();

        assertEquals(4, values.getValue()[0].getValue());
        assertEquals(3, values.getValue()[1].getValue());
        assertEquals(2, values.getValue()[2].getValue());
        assertEquals(1, values.getValue()[3].getValue());
    }

    @Test
    public void evaluateSortStrings() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("Charlie"),
                new StringLiteral("Alice"),
                new StringLiteral("Bob")
        });

        Sort sort = new Sort(array);

        ArrayValues<?> values = (ArrayValues<?>) sort.evaluate();

        assertEquals("Alice", values.getValue()[0].getValue());
        assertEquals("Bob", values.getValue()[1].getValue());
        assertEquals("Charlie", values.getValue()[2].getValue());
    }

    @Test
    public void evaluateSortByStringLength() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("AAAA"),
                new StringLiteral("A"),
                new StringLiteral("AAA"),
                new StringLiteral("AA")
        });

        Sort sort = new Sort(
                array,
                new StringLength(new Variable(ArrayOperation.ELEMENT_NAME)),
                BooleanLiteral.TRUE
        );

        ArrayValues<?> values = (ArrayValues<?>) sort.evaluate();

        assertEquals("A", values.getValue()[0].getValue());
        assertEquals("AA", values.getValue()[1].getValue());
        assertEquals("AAA", values.getValue()[2].getValue());
        assertEquals("AAAA", values.getValue()[3].getValue());
    }

    @Test
    public void evaluateSortByStringLengthDescending() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("AAAA"),
                new StringLiteral("A"),
                new StringLiteral("AAA"),
                new StringLiteral("AA")
        });

        Sort sort = new Sort(
                array,
                new StringLength(new Variable(ArrayOperation.ELEMENT_NAME)),
                BooleanLiteral.FALSE
        );

        ArrayValues<?> values = (ArrayValues<?>) sort.evaluate();

        assertEquals("AAAA", values.getValue()[0].getValue());
        assertEquals("AAA", values.getValue()[1].getValue());
        assertEquals("AA", values.getValue()[2].getValue());
        assertEquals("A", values.getValue()[3].getValue());
    }

    @Test
    public void evaluateDuplicates() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(3),
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(1)
        });

        Sort sort = new Sort(array);

        ArrayValues<?> values = (ArrayValues<?>) sort.evaluate();

        assertEquals(1, values.getValue()[0].getValue());
        assertEquals(1, values.getValue()[1].getValue());
        assertEquals(2, values.getValue()[2].getValue());
        assertEquals(3, values.getValue()[3].getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Sort sort = new Sort(array);

        ArrayValues<?> values = (ArrayValues<?>) sort.evaluate();

        assertEquals(0, values.getValue().length);
    }

    @Test
    public void evaluateSingleElement() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(42)
        });

        Sort sort = new Sort(array);

        ArrayValues<?> values = (ArrayValues<?>) sort.evaluate();

        assertEquals(1, values.getValue().length);
        assertEquals(42, values.getValue()[0].getValue());
    }

    @Test
    public void evaluateDoesNotModifyOriginalArray() {
        ArrayValues<?> array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(3),
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Sort sort = new Sort(array);

        sort.evaluate();

        assertEquals(3, array.getValue()[0].getValue());
        assertEquals(1, array.getValue()[1].getValue());
        assertEquals(2, array.getValue()[2].getValue());
    }

    @Test
    public void evaluateUnevaluatableArray() {
        Sort sort = new Sort(new Variable("array"));

        Node result = sort.evaluate();

        assertSame(sort, result);
    }
}
