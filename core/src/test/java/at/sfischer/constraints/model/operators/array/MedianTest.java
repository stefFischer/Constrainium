package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MedianTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Median median = new Median(array);

        ValidationContext context = new ValidationContext();
        median.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateNotAnArray() {
        Median median = new Median(new NumberLiteral(1));

        ValidationContext context = new ValidationContext();
        median.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateMissingArrayElementReference() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Median median = new Median(array, new NumberLiteral(5));

        ValidationContext context = new ValidationContext();
        median.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateOddNumberOfElements() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(10),
                new NumberLiteral(20),
                new NumberLiteral(30),
                new NumberLiteral(40),
                new NumberLiteral(50)
        });

        Median median = new Median(array);

        Node result = median.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(30.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateEvenNumberOfElements() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(10),
                new NumberLiteral(20),
                new NumberLiteral(30),
                new NumberLiteral(40)
        });

        Median median = new Median(array);

        Node result = median.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(25.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateSingleElement() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(42)
        });

        Median median = new Median(array);

        Node result = median.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(42.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Median median = new Median(array);

        Node result = median.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUnsortedArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(50),
                new NumberLiteral(10),
                new NumberLiteral(40),
                new NumberLiteral(20),
                new NumberLiteral(30)
        });

        Median median = new Median(array);

        Node result = median.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(30.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingKeySelector() {
        String json = """
                { array: [
                    {value:2}, {value:4}, {value:6}
                ]}
                """;
        Node array = DataObject.parseData(json).getDataValue("array").getLiteralValue();

        Median median = new Median(
                array,
                new Reference(
                        new Variable(ArrayOperation.ELEMENT_NAME),
                        new StringLiteral("value")));

        Node result = median.evaluate();

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

        Median median = new Median(
                array,
                new StringLength(
                        new Variable(ArrayOperation.ELEMENT_NAME)));

        Node result = median.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(2.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUnevaluatableArray() {
        Median median = new Median(new Variable("array"));

        Node result = median.evaluate();

        assertSame(median, result);
    }

    @Test
    public void evaluateUnevaluatableKeySelector() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Median median = new Median(
                array,
                new Variable("other"));

        Node result = median.evaluate();

        assertSame(median, result);
    }

    @Test
    public void cloneCreatesMedianInstance() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Median median = new Median(array);

        Node clone = median.cloneNode();

        assertInstanceOf(Median.class, clone);
        assertNotSame(median, clone);
    }

    @Test
    public void setVariableValuesCreatesMedianInstance() {
        Median median = new Median(new Variable("numbers"));

        Node result = median.setVariableValues(
                Map.of(
                        new Variable("numbers"),
                        new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3)
                        })
                )
        );

        assertInstanceOf(Median.class, result);
    }
}
