package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MinTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2)
        });

        Min min = new Min(array);

        ValidationContext context = new ValidationContext();
        min.validate(context);

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

        Min min = new Min(array);

        Node result = min.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(1.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingKeySelector() {
        String json = """
                { array: [
                    {value:4}, {value:2}, {value:7}
                ]}
                """;
        Node array = DataObject.parseData(json).getDataValue("array").getLiteralValue();

        Min min = new Min(
                array,
                new Reference(
                        new Variable(ArrayOperation.ELEMENT_NAME),
                        new StringLiteral("value")));

        Node result = min.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(2.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Min min = new Min(array);

        Node result = min.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUnevaluatableArray() {
        Min min = new Min(new Variable("array"));

        Node result = min.evaluate();

        assertSame(min, result);
    }

    @Test
    public void cloneCreatesMinInstance() {
        Min min = new Min(
                new ArrayValues<>(TypeEnum.NUMBER,
                        new NumberLiteral[]{new NumberLiteral(1)}));

        Node clone = min.cloneNode();

        assertInstanceOf(Min.class, clone);
        assertNotSame(min, clone);
    }

    @Test
    public void setVariableValuesCreatesMinInstance() {
        Min min = new Min(new Variable("numbers"));

        Node result = min.setVariableValues(Map.of(
                new Variable("numbers"),
                new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                        new NumberLiteral(1),
                        new NumberLiteral(2)
                })));

        assertInstanceOf(Min.class, result);
    }
}
