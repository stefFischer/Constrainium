package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MaxTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Max max = new Max(array);

        ValidationContext context = new ValidationContext();
        max.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void evaluateNumbers() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(8),
                new NumberLiteral(2),
                new NumberLiteral(5),
                new NumberLiteral(1)
        });

        Max max = new Max(array);

        Node result = max.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(8.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingKeySelector() {
        String json = """
                { array: [
                    {value:4}, {value:2}, {value:7}
                ]}
                """;
        Node array = DataObject.parseData(json).getDataValue("array").getLiteralValue();

        Max max = new Max(
                array,
                new Reference(
                        new Variable(ArrayOperation.ELEMENT_NAME),
                        new StringLiteral("value")));

        Node result = max.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(7.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Max max = new Max(array);

        Node result = max.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUnevaluatableArray() {
        Max max = new Max(new Variable("array"));

        Node result = max.evaluate();

        assertSame(max, result);
    }

    @Test
    public void cloneCreatesMaxInstance() {
        Max max = new Max(
                new ArrayValues<>(TypeEnum.NUMBER,
                        new NumberLiteral[]{new NumberLiteral(1)}));

        Node clone = max.cloneNode();

        assertInstanceOf(Max.class, clone);
        assertNotSame(max, clone);
    }

    @Test
    public void setVariableValuesCreatesMaxInstance() {
        Max max = new Max(new Variable("numbers"));

        Node result = max.setVariableValues(Map.of(
                new Variable("numbers"),
                new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                        new NumberLiteral(1),
                        new NumberLiteral(2)
                })));

        assertInstanceOf(Max.class, result);
    }
}
