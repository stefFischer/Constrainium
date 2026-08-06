package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.IsUrl;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FindIndexTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });
        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(1));

        FindIndex op = new FindIndex(array, predicate);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateMissingElementReference() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });
        Node predicate = new EqualOperator(
                new Variable("a"),
                new NumberLiteral(1));

        FindIndex op = new FindIndex(array, predicate);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateElementTypeMismatch() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });
        Node predicate = new StringEquals(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new StringLiteral("ONE"));

        FindIndex op = new FindIndex(array, predicate);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateNotAnArray() {
        Node array = new NumberLiteral(1);
        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(1));

        FindIndex op = new FindIndex(array, predicate);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateFindFirstOccurrence() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3),
                new NumberLiteral(2)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(2));

        FindIndex op = new FindIndex(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(1, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateFindFirstElement() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(5),
                new NumberLiteral(6),
                new NumberLiteral(7)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(5));

        FindIndex op = new FindIndex(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(0, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateFindLastElement() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(5),
                new NumberLiteral(6),
                new NumberLiteral(7)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(7));

        FindIndex op = new FindIndex(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(2, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateStringLength() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("THREE"),
                new StringLiteral("TWO")
        });

        Node predicate = new EqualOperator(
                new StringLength(new Variable(ArrayQuantifier.ELEMENT_NAME)),
                new IntegerLiteral(5));

        FindIndex op = new FindIndex(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(1, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateIsUrl() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("invalid"),
                new StringLiteral("https://github.com"),
                new StringLiteral("https://google.com")
        });

        Node predicate = new IsUrl(
                new Variable(ArrayQuantifier.ELEMENT_NAME));

        FindIndex op = new FindIndex(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(1, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateNoMatch() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(10));

        FindIndex op = new FindIndex(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(-1, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(1));

        FindIndex op = new FindIndex(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(-1, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateNestedFindIndex() {
        String jsonData = "{array:[{numbers:[0]},{numbers:[1,2]},{numbers:[3]}]}";
        DataObject obj = DataObject.parseData(jsonData);

        Node array = obj.getDataValue("array").getLiteralValue();

        Exists inner = new Exists(
                new Reference(
                        new Variable(ArrayQuantifier.ELEMENT_NAME),
                        new StringLiteral("numbers")),
                new EqualOperator(
                        new Variable(ArrayQuantifier.ELEMENT_NAME),
                        new NumberLiteral(2)));

        FindIndex outer = new FindIndex(array, inner);

        Node result = outer.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(1, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateUnevaluatablePredicate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new Variable("x"));

        FindIndex op = new FindIndex(array, predicate);

        Node result = op.evaluate();

        assertSame(op, result);
    }
}