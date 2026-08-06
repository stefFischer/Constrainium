package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.AdditionOperator;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ForEachTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });
        Node operation = new AdditionOperator(
                new Variable(ArrayOperation.ELEMENT_NAME),
                new NumberLiteral(1));

        ForEach op = new ForEach(array, operation);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateMissingElementReference() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });
        Node operation = new AdditionOperator(
                new Variable("a"),
                new NumberLiteral(1));

        ForEach op = new ForEach(array, operation);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateNotAnArray() {
        Node array = new NumberLiteral(1);
        Node operation = new AdditionOperator(
                new Variable(ArrayOperation.ELEMENT_NAME),
                new NumberLiteral(1));

        ForEach op = new ForEach(array, operation);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateIncrementNumbers() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Node operation = new AdditionOperator(
                new Variable(ArrayOperation.ELEMENT_NAME),
                new NumberLiteral(1));

        ForEach op = new ForEach(array, operation);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> values = (ArrayValues<?>) result;
        assertEquals(TypeEnum.NUMBER, values.getElementType());

        Value<?>[] actual = values.getValue();

        assertEquals(2.0, ((NumberLiteral) actual[0]).getValue());
        assertEquals(3.0, ((NumberLiteral) actual[1]).getValue());
        assertEquals(4.0, ((NumberLiteral) actual[2]).getValue());
    }

    @Test
    public void evaluateStringLength() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("THREE")
        });

        Node operation = new StringLength(new Variable(ArrayOperation.ELEMENT_NAME));
        ForEach op = new ForEach(array, operation);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> values = (ArrayValues<?>) result;
        assertEquals(TypeEnum.INTEGER, values.getElementType());

        Value<?>[] actual = values.getValue();

        assertEquals(3, ((IntegerLiteral) actual[0]).getValue());
        assertEquals(5, ((IntegerLiteral) actual[1]).getValue());
    }

    @Test
    public void evaluateIdentity() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("TWO")
        });

        Node operation = new Variable(ArrayOperation.ELEMENT_NAME);

        ForEach op = new ForEach(array, operation);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> values = (ArrayValues<?>) result;

        Value<?>[] actual = values.getValue();

        assertEquals("ONE", ((StringLiteral) actual[0]).getValue());
        assertEquals("TWO", ((StringLiteral) actual[1]).getValue());
    }

    @Test
    public void evaluateUnevaluatableOperation() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Node operation = new AdditionOperator(
                new Variable(ArrayOperation.ELEMENT_NAME),
                new Variable("x"));

        ForEach op = new ForEach(array, operation);

        Node result = op.evaluate();

        assertSame(op, result);
    }

    @Test
    public void evaluateNestedForEach() {
        String json =
                "{array:[{numbers:[1,2]},{numbers:[3]}]}";

        DataObject object = DataObject.parseData(json);

        Node array = object.getDataValue("array").getLiteralValue();

        Node inner = new ForEach(
                new Reference(
                        new Variable(ArrayOperation.ELEMENT_NAME),
                        new StringLiteral("numbers")),
                new AdditionOperator(
                        new Variable(ArrayOperation.ELEMENT_NAME),
                        new NumberLiteral(1)));

        ForEach op = new ForEach(array, inner);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> outer = (ArrayValues<?>) result;

        ArrayValues<?> first = (ArrayValues<?>) outer.getValue()[0];
        ArrayValues<?> second = (ArrayValues<?>) outer.getValue()[1];

        assertEquals(2.0, ((NumberLiteral) first.getValue()[0]).getValue());
        assertEquals(3.0, ((NumberLiteral) first.getValue()[1]).getValue());

        assertEquals(4.0, ((NumberLiteral) second.getValue()[0]).getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Node operation = new AdditionOperator(
                new Variable(ArrayOperation.ELEMENT_NAME),
                new NumberLiteral(1));

        ForEach op = new ForEach(array, operation);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> values = (ArrayValues<?>) result;

        assertEquals(0, values.getValue().length);
    }
}
