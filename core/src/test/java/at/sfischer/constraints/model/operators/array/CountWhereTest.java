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

public class CountWhereTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });
        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(1));

        CountWhere op = new CountWhere(array, predicate);

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

        CountWhere op = new CountWhere(array, predicate);

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

        CountWhere op = new CountWhere(array, predicate);

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

        CountWhere op = new CountWhere(array, predicate);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateCountMatchingNumbers() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3),
                new NumberLiteral(2)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(2));

        CountWhere op = new CountWhere(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(2, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateCountNoMatches() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(10));

        CountWhere op = new CountWhere(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(0, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateCountAllMatches() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(5),
                new NumberLiteral(5),
                new NumberLiteral(5)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(5));

        CountWhere op = new CountWhere(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(3, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateCountStringLength() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("THREE"),
                new StringLiteral("TWO"),
                new StringLiteral("FOUR")
        });

        Node predicate = new EqualOperator(
                new StringLength(new Variable(ArrayQuantifier.ELEMENT_NAME)),
                new IntegerLiteral(3));

        CountWhere op = new CountWhere(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(2, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateCountUrls() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("https://github.com"),
                new StringLiteral("invalid"),
                new StringLiteral("https://google.com"),
                new StringLiteral("not a url")
        });

        Node predicate = new IsUrl(
                new Variable(ArrayQuantifier.ELEMENT_NAME));

        CountWhere op = new CountWhere(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(2, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(1));

        CountWhere op = new CountWhere(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(0, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateNestedCountWhere() {
        String jsonData = "{array:[{numbers:[1,2,3]},{numbers:[2,4]},{numbers:[5]}]}";
        DataObject obj = DataObject.parseData(jsonData);

        Node array = obj.getDataValue("array").getLiteralValue();

        CountWhere inner = new CountWhere(
                new Reference(
                        new Variable(ArrayQuantifier.ELEMENT_NAME),
                        new StringLiteral("numbers")),
                new EqualOperator(
                        new Variable(ArrayQuantifier.ELEMENT_NAME),
                        new NumberLiteral(2)));

        CountWhere outer = new CountWhere(array, new EqualOperator(inner, new NumberLiteral(1)));

        Node result = outer.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(2, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateUnevaluatablePredicate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new Variable("x"));

        CountWhere op = new CountWhere(array, predicate);

        Node result = op.evaluate();

        assertSame(op, result);
    }
}
