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

public class FilterTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });
        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(1));

        Filter op = new Filter(array, predicate);

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

        Filter op = new Filter(array, predicate);

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

        Filter op = new Filter(array, predicate);

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

        Filter op = new Filter(array, predicate);

        ValidationContext context = new ValidationContext();
        op.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateFilterNumbers() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3),
                new NumberLiteral(4)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(2));

        Filter op = new Filter(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(TypeEnum.NUMBER, resultArray.getElementType());
        assertEquals(1, resultArray.getValue().length);
        assertEquals(2, resultArray.getValue()[0].getValue());
    }

    @Test
    public void evaluateFilterMultipleMatches() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(2));

        Filter op = new Filter(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(2, resultArray.getValue().length);
        assertEquals(2, resultArray.getValue()[0].getValue());
        assertEquals(2, resultArray.getValue()[1].getValue());
    }

    @Test
    public void evaluateFilterNoMatches() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(10));

        Filter op = new Filter(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(0, resultArray.getValue().length);
    }

    @Test
    public void evaluateFilterAllMatches() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("TWO"),
                new StringLiteral("THREE")
        });

        Node predicate = new IsUrl(
                new Variable(ArrayQuantifier.ELEMENT_NAME));

        Filter op = new Filter(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(0, resultArray.getValue().length);
    }

    @Test
    public void evaluateFilterStringLength() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("ONE"),
                new StringLiteral("THREE"),
                new StringLiteral("TWO")
        });

        Node predicate = new EqualOperator(
                new StringLength(new Variable(ArrayQuantifier.ELEMENT_NAME)),
                new IntegerLiteral(3));

        Filter op = new Filter(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(2, resultArray.getValue().length);
        assertEquals("ONE", resultArray.getValue()[0].getValue());
        assertEquals("TWO", resultArray.getValue()[1].getValue());
    }

    @Test
    public void evaluateFilterUrls() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("https://github.com"),
                new StringLiteral("invalid"),
                new StringLiteral("https://google.com")
        });

        Node predicate = new IsUrl(
                new Variable(ArrayQuantifier.ELEMENT_NAME));

        Filter op = new Filter(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(2, resultArray.getValue().length);
        assertEquals("https://github.com", resultArray.getValue()[0].getValue());
        assertEquals("https://google.com", resultArray.getValue()[1].getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new NumberLiteral(1));

        Filter op = new Filter(array, predicate);

        Node result = op.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(0, resultArray.getValue().length);
    }

    @Test
    public void evaluateNestedFilter() {
        String jsonData = "{array:[{numbers:[1,2,3]},{numbers:[2,4]},{numbers:[5]}]}";
        DataObject obj = DataObject.parseData(jsonData);

        Node array = obj.getDataValue("array").getLiteralValue();

        Exists inner = new Exists(
                new Reference(
                        new Variable(ArrayQuantifier.ELEMENT_NAME),
                        new StringLiteral("numbers")),
                new EqualOperator(
                        new Variable(ArrayQuantifier.ELEMENT_NAME),
                        new NumberLiteral(2)));

        Filter outer = new Filter(array, inner);

        Node result = outer.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> resultArray = (ArrayValues<?>) result;
        assertEquals(2, resultArray.getValue().length);
    }

    @Test
    public void evaluateUnevaluatablePredicate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1)
        });

        Node predicate = new EqualOperator(
                new Variable(ArrayQuantifier.ELEMENT_NAME),
                new Variable("x"));

        Filter op = new Filter(array, predicate);

        Node result = op.evaluate();

        assertSame(op, result);
    }
}