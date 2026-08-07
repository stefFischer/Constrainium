package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SumTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Sum sum = new Sum(array);

        ValidationContext context = new ValidationContext();
        sum.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateNotAnArray() {
        Sum sum = new Sum(new NumberLiteral(1));

        ValidationContext context = new ValidationContext();
        sum.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateMissingArrayElementReference() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Sum sum = new Sum(array, new NumberLiteral(5));

        ValidationContext context = new ValidationContext();
        sum.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateNumbers() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1.5),
                new NumberLiteral(2.0),
                new NumberLiteral(3.5)
        });

        Sum sum = new Sum(array);

        Node result = sum.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(7.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateIntegers() {
        Node array = new ArrayValues<>(TypeEnum.INTEGER, new IntegerLiteral[]{
                new IntegerLiteral(1),
                new IntegerLiteral(2),
                new IntegerLiteral(3),
                new IntegerLiteral(4)
        });

        Sum sum = new Sum(array);

        Node result = sum.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(10, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateEmptyNumberArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Sum sum = new Sum(array);

        Node result = sum.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateEmptyIntegerArray() {
        Node array = new ArrayValues<>(TypeEnum.INTEGER, new IntegerLiteral[]{});

        Sum sum = new Sum(array);

        Node result = sum.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(0, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateSingleElement() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(42.5)
        });

        Sum sum = new Sum(array);

        Node result = sum.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(42.5, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingKeySelector() {
        String json = """
                { array: [
                    {value:2}, {value:3}, {value:5}
                ]}
                """;
        Node array = DataObject.parseData(json).getDataValue("array").getLiteralValue();

        Sum sum = new Sum(
                array,
                new Reference(
                        new Variable(ArrayOperation.ELEMENT_NAME),
                        new StringLiteral("value"))
        );

        Node result = sum.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(10, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingDerivedKeySelector() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("A"),
                new StringLiteral("BB"),
                new StringLiteral("CCC")
        });

        Sum sum = new Sum(
                array,
                new StringLength(new Variable(ArrayOperation.ELEMENT_NAME))
        );

        Node result = sum.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(6, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateUnevaluatableArray() {
        Sum sum = new Sum(new Variable("array"));

        Node result = sum.evaluate();

        assertSame(sum, result);
    }

    @Test
    public void evaluateUnevaluatableKeySelector() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Sum sum = new Sum(
                array,
                new Variable("other")
        );

        Node result = sum.evaluate();

        assertSame(sum, result);
    }

    @Test
    public void evaluateOriginalArrayUnchanged() {
        ArrayValues<?> array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Sum sum = new Sum(array);

        sum.evaluate();

        assertEquals(3, array.getValue().length);
        assertEquals(1, array.getValue()[0].getValue());
        assertEquals(2, array.getValue()[1].getValue());
        assertEquals(3, array.getValue()[2].getValue());
    }
}
