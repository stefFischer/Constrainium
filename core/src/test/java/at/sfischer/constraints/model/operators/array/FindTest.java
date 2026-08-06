package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.IsUrl;
import at.sfischer.constraints.model.operators.strings.OneOfString;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FindTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });
        Node predicate = new EqualOperator(
                new Variable(Find.ELEMENT_NAME),
                new NumberLiteral(1));

        Find find = new Find(array, predicate);

        ValidationContext context = new ValidationContext();
        find.validate(context);

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

        Find find = new Find(array, predicate);

        ValidationContext context = new ValidationContext();
        find.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateElementTypeMismatch() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });
        Node predicate = new StringEquals(
                new Variable(Find.ELEMENT_NAME),
                new StringLiteral("ONE"));

        Find find = new Find(array, predicate);

        ValidationContext context = new ValidationContext();
        find.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateNotAnArray() {
        Node array = new NumberLiteral(1);
        Node predicate = new EqualOperator(
                new Variable(Find.ELEMENT_NAME),
                new NumberLiteral(1));

        Find find = new Find(array, predicate);

        ValidationContext context = new ValidationContext();
        find.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateFindFirstNumber() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3),
                new NumberLiteral(2)
        });

        Node predicate = new EqualOperator(
                new Variable(Find.ELEMENT_NAME),
                new NumberLiteral(2));

        Find find = new Find(array, predicate);

        Node result = find.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(2, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateFindFirstString() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("TWO"),
                new StringLiteral("THREE")
        });

        Node predicate = new StringEquals(
                new Variable(Find.ELEMENT_NAME),
                new StringLiteral("THREE"));

        Find find = new Find(array, predicate);

        Node result = find.evaluate();

        assertInstanceOf(StringLiteral.class, result);
        assertEquals("THREE", ((StringLiteral) result).getValue());
    }

    @Test
    public void evaluateFindUsingOneOfString() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("FOUR"),
                new StringLiteral("TWO")
        });

        Node predicate = new OneOfString(
                new Variable(Find.ELEMENT_NAME),
                new IntegerLiteral(3));

        Find find = new Find(array, predicate);

        Node result = find.evaluate();

        assertInstanceOf(StringLiteral.class, result);
        assertEquals("ONE", ((StringLiteral) result).getValue());
    }

    @Test
    public void evaluateFindUsingStringLength() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("THREE"),
                new StringLiteral("TWO")
        });

        Node predicate = new EqualOperator(
                new StringLength(new Variable(Find.ELEMENT_NAME)),
                new IntegerLiteral(5));

        Find find = new Find(array, predicate);

        Node result = find.evaluate();

        assertInstanceOf(StringLiteral.class, result);
        assertEquals("THREE", ((StringLiteral) result).getValue());
    }

    @Test
    public void evaluateFindUrl() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("not a url"),
                new StringLiteral("https://github.com"),
                new StringLiteral("https://www.google.com")
        });

        Node predicate = new IsUrl(
                new Variable(Find.ELEMENT_NAME));

        Find find = new Find(array, predicate);

        Node result = find.evaluate();

        assertInstanceOf(StringLiteral.class, result);
        assertEquals("https://github.com", ((StringLiteral) result).getValue());
    }

    @Test
    public void evaluateNoMatch() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Node predicate = new EqualOperator(
                new Variable(Find.ELEMENT_NAME),
                new NumberLiteral(5));

        Find find = new Find(array, predicate);

        Node result = find.evaluate();

        assertSame(NullLiteral.INSTANCE, result);
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Node predicate = new EqualOperator(
                new Variable(Find.ELEMENT_NAME),
                new NumberLiteral(1));

        Find find = new Find(array, predicate);

        Node result = find.evaluate();

        assertSame(NullLiteral.INSTANCE, result);
    }

    @Test
    public void evaluateNestedFind() {
        String jsonData = "{array:[{numbers:[0]},{numbers:[1,2]},{numbers:[3]}]}";
        DataObject obj = DataObject.parseData(jsonData);

        Node array = obj.getDataValue("array").getLiteralValue();
        Exists inner = new Exists(
                new Reference(
                        new Variable(Find.ELEMENT_NAME),
                        new StringLiteral("numbers")),
                new EqualOperator(
                        new Variable(Find.ELEMENT_NAME),
                        new NumberLiteral(2)));

        Find outer = new Find(array, inner);
        Node result = outer.evaluate();

        assertInstanceOf(ComplexValue.class, result);
    }

    @Test
    public void evaluateUnevaluatablePredicate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Node predicate = new EqualOperator(
                new Variable(Find.ELEMENT_NAME),
                new Variable("x"));

        Find find = new Find(array, predicate);

        Node result = find.evaluate();

        assertSame(find, result);
    }
}
