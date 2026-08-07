package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StandardDeviationTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        StandardDeviation sd = new StandardDeviation(array);

        ValidationContext context = new ValidationContext();
        sd.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateNotAnArray() {
        StandardDeviation sd = new StandardDeviation(new NumberLiteral(1));

        ValidationContext context = new ValidationContext();
        sd.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateMissingArrayElementReference() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        StandardDeviation sd = new StandardDeviation(array, new NumberLiteral(5));

        ValidationContext context = new ValidationContext();
        sd.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateNumbers() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2),
                new NumberLiteral(4),
                new NumberLiteral(6),
                new NumberLiteral(8)
        });

        StandardDeviation sd = new StandardDeviation(array);

        Node result = sd.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(Math.sqrt(5.0), ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateIntegers() {
        Node array = new ArrayValues<>(TypeEnum.INTEGER, new IntegerLiteral[]{
                new IntegerLiteral(1),
                new IntegerLiteral(2),
                new IntegerLiteral(3)
        });

        StandardDeviation sd = new StandardDeviation(array);

        Node result = sd.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(Math.sqrt(2.0 / 3.0), ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateSingleElement() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(42)
        });

        StandardDeviation sd = new StandardDeviation(array);

        Node result = sd.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        StandardDeviation sd = new StandardDeviation(array);

        Node result = sd.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingKeySelector() {
        String json = """
                { array: [
                    {value:2}, {value:4}, {value:6}
                ]}
                """;
        Node array = DataObject.parseData(json).getDataValue("array").getLiteralValue();

        StandardDeviation sd = new StandardDeviation(
                array,
                new Reference(
                        new Variable(ArrayOperation.ELEMENT_NAME),
                        new StringLiteral("value"))
        );

        Node result = sd.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(Math.sqrt(8.0 / 3.0), ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingDerivedKeySelector() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("A"),
                new StringLiteral("BB"),
                new StringLiteral("CCC")
        });

        StandardDeviation sd = new StandardDeviation(
                array,
                new StringLength(
                        new Variable(ArrayOperation.ELEMENT_NAME))
        );

        Node result = sd.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(Math.sqrt(2.0 / 3.0), ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateAllEqual() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(5),
                new NumberLiteral(5),
                new NumberLiteral(5),
                new NumberLiteral(5)
        });

        StandardDeviation sd = new StandardDeviation(array);

        Node result = sd.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateNegativeNumbers() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(-2),
                new NumberLiteral(0),
                new NumberLiteral(2)
        });

        StandardDeviation sd = new StandardDeviation(array);

        Node result = sd.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(Math.sqrt(8.0 / 3.0), ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUnevaluatableArray() {
        StandardDeviation sd = new StandardDeviation(new Variable("array"));

        Node result = sd.evaluate();

        assertSame(sd, result);
    }

    @Test
    public void evaluateUnevaluatableKeySelector() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        StandardDeviation sd = new StandardDeviation(
                array,
                new Variable("other")
        );

        Node result = sd.evaluate();

        assertSame(sd, result);
    }

    @Test
    public void evaluateOriginalArrayUnchanged() {
        ArrayValues<?> array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        StandardDeviation sd = new StandardDeviation(array);

        sd.evaluate();

        assertEquals(3, array.getValue().length);
        assertEquals(1, array.getValue()[0].getValue());
        assertEquals(2, array.getValue()[1].getValue());
        assertEquals(3, array.getValue()[2].getValue());
    }

    @Test
    public void cloneCreatesStandardDeviationInstance() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        StandardDeviation sd = new StandardDeviation(array);

        Node clone = sd.cloneNode();

        assertInstanceOf(StandardDeviation.class, clone);
        assertNotSame(sd, clone);
    }

    @Test
    public void setVariableValuesCreatesStandardDeviationInstance() {
        StandardDeviation sd = new StandardDeviation(new Variable("numbers"));

        Node result = sd.setVariableValues(
                Map.of(
                        new Variable("numbers"),
                        new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3)
                        })
                )
        );

        assertInstanceOf(StandardDeviation.class, result);
    }

    @Test
    public void evaluateWikipediaExample() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(600),
                new NumberLiteral(470),
                new NumberLiteral(170),
                new NumberLiteral(430),
                new NumberLiteral(300)
        });

        StandardDeviation sd = new StandardDeviation(array);

        Node result = sd.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(Math.sqrt(21704.0), ((NumberLiteral) result).getValue());
    }
}
