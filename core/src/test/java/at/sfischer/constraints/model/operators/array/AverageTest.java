package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AverageTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Average average = new Average(array);

        ValidationContext context = new ValidationContext();
        average.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateNotAnArray() {
        Average average = new Average(new NumberLiteral(1));

        ValidationContext context = new ValidationContext();
        average.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateMissingArrayElementReference() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Average average = new Average(array, new NumberLiteral(5));

        ValidationContext context = new ValidationContext();
        average.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateNumbers() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2.0),
                new NumberLiteral(4.0),
                new NumberLiteral(6.0)
        });

        Average average = new Average(array);

        Node result = average.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(4.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateIntegers() {
        Node array = new ArrayValues<>(TypeEnum.INTEGER, new IntegerLiteral[]{
                new IntegerLiteral(2),
                new IntegerLiteral(4),
                new IntegerLiteral(6)
        });

        Average average = new Average(array);

        Node result = average.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(4.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateFractionalAverage() {
        Node array = new ArrayValues<>(TypeEnum.INTEGER, new IntegerLiteral[]{
                new IntegerLiteral(1),
                new IntegerLiteral(2)
        });

        Average average = new Average(array);

        Node result = average.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(1.5, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Average average = new Average(array);

        Node result = average.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateSingleElement() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(42.5)
        });

        Average average = new Average(array);

        Node result = average.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(42.5, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingKeySelector() {
        String json = """
                { array: [
                    {value:2}, {value:4}, {value:6}
                ]}
                """;
        Node array = DataObject.parseData(json).getDataValue("array").getLiteralValue();

        Average average = new Average(
                array,
                new Reference(
                        new Variable(ArrayOperation.ELEMENT_NAME),
                        new StringLiteral("value"))
        );

        Node result = average.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(4.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingDerivedKeySelector() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("A"),
                new StringLiteral("BB"),
                new StringLiteral("CCC")
        });

        Average average = new Average(
                array,
                new StringLength(
                        new Variable(ArrayOperation.ELEMENT_NAME))
        );

        Node result = average.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(2.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUnevaluatableArray() {
        Average average = new Average(new Variable("array"));

        Node result = average.evaluate();

        assertSame(average, result);
    }

    @Test
    public void evaluateUnevaluatableKeySelector() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Average average = new Average(
                array,
                new Variable("other")
        );

        Node result = average.evaluate();

        assertSame(average, result);
    }

    @Test
    public void evaluateOriginalArrayUnchanged() {
        ArrayValues<?> array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Average average = new Average(array);

        average.evaluate();

        assertEquals(3, array.getValue().length);
        assertEquals(1, array.getValue()[0].getValue());
        assertEquals(2, array.getValue()[1].getValue());
        assertEquals(3, array.getValue()[2].getValue());
    }
}
