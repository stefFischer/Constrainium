package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PercentileTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Percentile percentile = new Percentile(array, new NumberLiteral(50));

        ValidationContext context = new ValidationContext();
        percentile.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateNotAnArray() {
        Percentile percentile = new Percentile(
                new NumberLiteral(1),
                new NumberLiteral(50));

        ValidationContext context = new ValidationContext();
        percentile.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateMissingArrayElementReference() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Percentile percentile = new Percentile(
                array,
                new NumberLiteral(5),
                new NumberLiteral(50));

        ValidationContext context = new ValidationContext();
        percentile.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateMedianOdd() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(10),
                new NumberLiteral(20),
                new NumberLiteral(30),
                new NumberLiteral(40),
                new NumberLiteral(50)
        });

        Percentile percentile = new Percentile(array, new NumberLiteral(50));

        Node result = percentile.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(30.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateMedianEven() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(10),
                new NumberLiteral(20),
                new NumberLiteral(30),
                new NumberLiteral(40)
        });

        Percentile percentile = new Percentile(array, new NumberLiteral(50));

        Node result = percentile.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(25.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateZeroPercentile() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(30),
                new NumberLiteral(10),
                new NumberLiteral(20)
        });

        Percentile percentile = new Percentile(array, new NumberLiteral(0));

        Node result = percentile.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(10.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateHundredPercentile() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(30),
                new NumberLiteral(10),
                new NumberLiteral(20)
        });

        Percentile percentile = new Percentile(array, new NumberLiteral(100));

        Node result = percentile.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(30.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateInterpolatedPercentile() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(10),
                new NumberLiteral(20),
                new NumberLiteral(30),
                new NumberLiteral(40)
        });

        Percentile percentile = new Percentile(array, new NumberLiteral(75));

        Node result = percentile.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(32.5, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateSingleElement() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(42)
        });

        Percentile percentile = new Percentile(array, new NumberLiteral(25));

        Node result = percentile.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(42.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Percentile percentile = new Percentile(array, new NumberLiteral(50));

        Node result = percentile.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingKeySelector() {
        String json = """
                { array: [
                    {value:2}, {value:4}, {value:6}
                ]}
                """;
        Node array = DataObject.parseData(json).getDataValue("array").getLiteralValue();

        Percentile percentile = new Percentile(
                array,
                new Reference(
                        new Variable(ArrayOperation.ELEMENT_NAME),
                        new StringLiteral("value")),
                new NumberLiteral(50));

        Node result = percentile.evaluate();

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

        Percentile percentile = new Percentile(
                array,
                new StringLength(new Variable(ArrayOperation.ELEMENT_NAME)),
                new NumberLiteral(50));

        Node result = percentile.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(2.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUnsortedInput() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(50),
                new NumberLiteral(10),
                new NumberLiteral(40),
                new NumberLiteral(20),
                new NumberLiteral(30)
        });

        Percentile percentile = new Percentile(array, new NumberLiteral(50));

        Node result = percentile.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(30.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateInvalidPercentileLow() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Percentile percentile = new Percentile(array, new NumberLiteral(-1));

        Node result = percentile.evaluate();

        assertSame(percentile, result);
    }

    @Test
    public void evaluateInvalidPercentileHigh() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Percentile percentile = new Percentile(array, new NumberLiteral(101));

        Node result = percentile.evaluate();

        assertSame(percentile, result);
    }

    @Test
    public void evaluateUnevaluatableArray() {
        Percentile percentile = new Percentile(
                new Variable("array"),
                new NumberLiteral(50));

        Node result = percentile.evaluate();

        assertSame(percentile, result);
    }

    @Test
    public void evaluateUnevaluatableKeySelector() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Percentile percentile = new Percentile(
                array,
                new Variable("other"),
                new NumberLiteral(50));

        Node result = percentile.evaluate();

        assertSame(percentile, result);
    }

    @Test
    public void cloneCreatesPercentileInstance() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Percentile percentile = new Percentile(array, new NumberLiteral(50));

        Node clone = percentile.cloneNode();

        assertInstanceOf(Percentile.class, clone);
        assertNotSame(percentile, clone);
    }

    @Test
    public void setVariableValuesCreatesPercentileInstance() {
        Percentile percentile = new Percentile(
                new Variable("numbers"),
                new NumberLiteral(50));

        Node result = percentile.setVariableValues(
                Map.of(
                        new Variable("numbers"),
                        new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3)
                        })
                )
        );

        assertInstanceOf(Percentile.class, result);
    }
}
