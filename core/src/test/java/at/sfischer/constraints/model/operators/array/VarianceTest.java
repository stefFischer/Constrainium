package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VarianceTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Variance variance = new Variance(array);

        ValidationContext context = new ValidationContext();
        variance.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateNotAnArray() {
        Variance variance = new Variance(new NumberLiteral(1));

        ValidationContext context = new ValidationContext();
        variance.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateMissingArrayElementReference() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Variance variance = new Variance(array, new NumberLiteral(5));

        ValidationContext context = new ValidationContext();
        variance.validate(context);

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

        Variance variance = new Variance(array);

        Node result = variance.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(5.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateIntegers() {
        Node array = new ArrayValues<>(TypeEnum.INTEGER, new IntegerLiteral[]{
                new IntegerLiteral(1),
                new IntegerLiteral(2),
                new IntegerLiteral(3)
        });

        Variance variance = new Variance(array);

        Node result = variance.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(2.0 / 3.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateSingleElement() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(42)
        });

        Variance variance = new Variance(array);

        Node result = variance.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Variance variance = new Variance(array);

        Node result = variance.evaluate();

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

        Variance variance = new Variance(
                array,
                new Reference(
                        new Variable(ArrayOperation.ELEMENT_NAME),
                        new StringLiteral("value"))
        );

        Node result = variance.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(8.0 / 3.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingDerivedKeySelector() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("A"),
                new StringLiteral("BB"),
                new StringLiteral("CCC")
        });

        Variance variance = new Variance(
                array,
                new StringLength(
                        new Variable(ArrayOperation.ELEMENT_NAME))
        );

        Node result = variance.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(2.0 / 3.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateAllEqual() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(5),
                new NumberLiteral(5),
                new NumberLiteral(5),
                new NumberLiteral(5)
        });

        Variance variance = new Variance(array);

        Node result = variance.evaluate();

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

        Variance variance = new Variance(array);

        Node result = variance.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(8.0 / 3.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUnevaluatableArray() {
        Variance variance = new Variance(new Variable("array"));

        Node result = variance.evaluate();

        assertSame(variance, result);
    }

    @Test
    public void evaluateUnevaluatableKeySelector() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Variance variance = new Variance(
                array,
                new Variable("other")
        );

        Node result = variance.evaluate();

        assertSame(variance, result);
    }

    @Test
    public void evaluateOriginalArrayUnchanged() {
        ArrayValues<?> array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Variance variance = new Variance(array);

        variance.evaluate();

        assertEquals(3, array.getValue().length);
        assertEquals(1, array.getValue()[0].getValue());
        assertEquals(2, array.getValue()[1].getValue());
        assertEquals(3, array.getValue()[2].getValue());
    }

    @Test
    public void cloneCreatesVarianceInstance() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Variance variance = new Variance(array);

        Node clone = variance.cloneNode();

        assertInstanceOf(Variance.class, clone);
        assertNotSame(variance, clone);
    }

    @Test
    public void setVariableValuesCreatesVarianceInstance() {
        Variance variance = new Variance(new Variable("numbers"));

        Node result = variance.setVariableValues(
                Map.of(
                        new Variable("numbers"),
                        new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                                new NumberLiteral(1),
                                new NumberLiteral(2),
                                new NumberLiteral(3)
                        })
                )
        );

        assertInstanceOf(Variance.class, result);
    }
}
