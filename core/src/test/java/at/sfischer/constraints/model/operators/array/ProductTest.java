package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringLength;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProductTest {

    @Test
    public void validate() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Product product = new Product(array);

        ValidationContext context = new ValidationContext();
        product.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    public void validateNotAnArray() {
        Product product = new Product(new NumberLiteral(1));

        ValidationContext context = new ValidationContext();
        product.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void validateMissingArrayElementReference() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2)
        });

        Product product = new Product(array, new NumberLiteral(5));

        ValidationContext context = new ValidationContext();
        product.validate(context);

        assertFalse(context.isValid());
        assertEquals(1, context.getMessages().size());
    }

    @Test
    public void evaluateNumbers() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2.5),
                new NumberLiteral(4.0)
        });

        Product product = new Product(array);

        Node result = product.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(10.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateIntegers() {
        Node array = new ArrayValues<>(TypeEnum.INTEGER, new IntegerLiteral[]{
                new IntegerLiteral(2),
                new IntegerLiteral(3),
                new IntegerLiteral(4)
        });

        Product product = new Product(array);

        Node result = product.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(24, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateMixedNumberTypes() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new Value[]{
                new IntegerLiteral(2),
                new NumberLiteral(2.5),
                new IntegerLiteral(4)
        });

        Product product = new Product(array);

        Node result = product.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(20.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateEmptyArray() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{});

        Product product = new Product(array);

        Node result = product.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(1, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateSingleElement() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(42)
        });

        Product product = new Product(array);

        Node result = product.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(42.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingKeySelector() {
        String json = """
                { array: [
                    {value:2}, {value:3}, {value:5}
                ]}
                """;
        Node array = DataObject.parseData(json).getDataValue("array").getLiteralValue();

        Product product = new Product(
                array,
                new Reference(
                        new Variable(ArrayOperation.ELEMENT_NAME),
                        new StringLiteral("value"))
        );

        Node result = product.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(30, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateUsingDerivedKeySelector() {
        Node array = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("A"),
                new StringLiteral("BB"),
                new StringLiteral("CCC")
        });

        Product product = new Product(
                array,
                new StringLength(new Variable(ArrayOperation.ELEMENT_NAME))
        );

        Node result = product.evaluate();

        assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(6, ((IntegerLiteral) result).getValue());
    }

    @Test
    public void evaluateZeroElement() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2),
                new NumberLiteral(0),
                new NumberLiteral(5)
        });

        Product product = new Product(array);

        Node result = product.evaluate();

        assertInstanceOf(NumberLiteral.class, result);
        assertEquals(0.0, ((NumberLiteral) result).getValue());
    }

    @Test
    public void evaluateUnevaluatableArray() {
        Product product = new Product(new Variable("array"));

        Node result = product.evaluate();

        assertSame(product, result);
    }

    @Test
    public void evaluateUnevaluatableKeySelector() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2)
        });

        Product product = new Product(
                array,
                new Variable("other")
        );

        Node result = product.evaluate();

        assertSame(product, result);
    }

    @Test
    public void evaluateOriginalArrayUnchanged() {
        ArrayValues<?> array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2),
                new NumberLiteral(3),
                new NumberLiteral(4)
        });

        Product product = new Product(array);

        product.evaluate();

        assertEquals(3, array.getValue().length);
        assertEquals(2, array.getValue()[0].getValue());
        assertEquals(3, array.getValue()[1].getValue());
        assertEquals(4, array.getValue()[2].getValue());
    }

    @Test
    public void cloneCreatesProductInstance() {
        Node array = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(2),
                new NumberLiteral(3)
        });

        Product product = new Product(array);

        Node clone = product.cloneNode();

        assertInstanceOf(Product.class, clone);
        assertNotSame(product, clone);
    }

    @Test
    public void setVariableValuesCreatesProductInstance() {
        Product product = new Product(
                new Variable("numbers")
        );

        Node result = product.setVariableValues(
                Map.of(
                        new Variable("numbers"),
                        new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                                new NumberLiteral(2),
                                new NumberLiteral(3)
                        })
                )
        );

        assertInstanceOf(Product.class, result);
    }
}
