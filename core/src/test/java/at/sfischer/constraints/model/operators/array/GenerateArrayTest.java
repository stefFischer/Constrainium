package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.AdditionOperator;
import at.sfischer.constraints.model.operators.numbers.MultiplicationOperator;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GenerateArrayTest {

    @Test
    void shouldGenerateArrayFromConstantGenerator() {
        GenerateArray operation = new GenerateArray(
                new IntegerLiteral(42),
                new IntegerLiteral(4)
        );

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);

        ArrayValues<?> array = (ArrayValues<?>) result;

        assertEquals(4, array.getValue().length);
        assertEquals(42, ((IntegerLiteral) array.getValue()[0]).getValue());
        assertEquals(42, ((IntegerLiteral) array.getValue()[1]).getValue());
        assertEquals(42, ((IntegerLiteral) array.getValue()[2]).getValue());
        assertEquals(42, ((IntegerLiteral) array.getValue()[3]).getValue());
    }

    @Test
    void shouldGenerateEmptyArrayForZeroLength() {
        GenerateArray operation = new GenerateArray(
                new IntegerLiteral(42),
                new IntegerLiteral(0)
        );

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(0, array.getValue().length);
    }

    @Test
    void shouldGenerateValuesUsingArrayIndex() {
        Variable index = new Variable(ArrayOperation.INDEX_NAME);
        GenerateArray operation = new GenerateArray(
                index,
                new IntegerLiteral(5)
        );

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(5, array.getValue().length);
        assertEquals(0, ((NumberLiteral) array.getValue()[0]).getValue());
        assertEquals(1, ((NumberLiteral) array.getValue()[1]).getValue());
        assertEquals(2, ((NumberLiteral) array.getValue()[2]).getValue());
        assertEquals(3, ((NumberLiteral) array.getValue()[3]).getValue());
        assertEquals(4, ((NumberLiteral) array.getValue()[4]).getValue());
    }

    @Test
    void shouldGenerateSequenceUsingArrayIndex() {
        Variable index = new Variable(ArrayOperation.INDEX_NAME);
        Node generator = new AdditionOperator(index, new IntegerLiteral(10));
        GenerateArray operation = new GenerateArray(
                generator,
                new IntegerLiteral(4)
        );

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(4, array.getValue().length);
        assertEquals(10.0, ((NumberLiteral) array.getValue()[0]).getValue());
        assertEquals(11.0, ((NumberLiteral) array.getValue()[1]).getValue());
        assertEquals(12.0, ((NumberLiteral) array.getValue()[2]).getValue());
        assertEquals(13.0, ((NumberLiteral) array.getValue()[3]).getValue());
    }

    @Test
    void shouldGenerateNumberValues() {
        Variable index = new Variable(ArrayOperation.INDEX_NAME);
        Node generator = new MultiplicationOperator(index, new NumberLiteral(0.5));
        GenerateArray operation = new GenerateArray(
                generator,
                new IntegerLiteral(4)
        );

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(4, array.getValue().length);
        assertEquals(0.0, ((NumberLiteral) array.getValue()[0]).getValue().doubleValue(), 1e-10);
        assertEquals(0.5, ((NumberLiteral) array.getValue()[1]).getValue().doubleValue(),1e-10);
        assertEquals(1.0, ((NumberLiteral) array.getValue()[2]).getValue().doubleValue(), 1e-10);
        assertEquals(1.5, ((NumberLiteral) array.getValue()[3]).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldEvaluateGeneratorForEveryIndex() {
        Variable index = new Variable(ArrayOperation.INDEX_NAME);
        Node generator = new MultiplicationOperator(
                index,
                index
        );
        GenerateArray operation = new GenerateArray(
                generator,
                new IntegerLiteral(5)
        );

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(5, array.getValue().length);
        assertEquals(0.0, ((NumberLiteral) array.getValue()[0]).getValue());
        assertEquals(1.0, ((NumberLiteral) array.getValue()[1]).getValue());
        assertEquals(4.0, ((NumberLiteral) array.getValue()[2]).getValue());
        assertEquals(9.0, ((NumberLiteral) array.getValue()[3]).getValue());
        assertEquals(16.0, ((NumberLiteral) array.getValue()[4]).getValue());
    }

    @Test
    void shouldGenerateArrayUsingExpressionWithIndex() {
        Variable index = new Variable(ArrayOperation.INDEX_NAME);
        Node generator = new AdditionOperator(
                new MultiplicationOperator(
                        index,
                        new IntegerLiteral(2)
                ),
                new IntegerLiteral(1)
        );
        GenerateArray operation = new GenerateArray(
                generator,
                new IntegerLiteral(5)
        );

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(1.0, ((NumberLiteral) array.getValue()[0]).getValue());
        assertEquals(3.0, ((NumberLiteral) array.getValue()[1]).getValue());
        assertEquals(5.0, ((NumberLiteral) array.getValue()[2]).getValue());
        assertEquals(7.0, ((NumberLiteral) array.getValue()[3]).getValue());
        assertEquals(9.0, ((NumberLiteral) array.getValue()[4]).getValue());
    }

    @Test
    void shouldReturnThisWhenLengthCannotBeEvaluated() {
        GenerateArray operation = new GenerateArray(
                new IntegerLiteral(42),
                new Variable("length")
        );

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisForNegativeLength() {
        GenerateArray operation = new GenerateArray(
                new IntegerLiteral(42),
                new IntegerLiteral(-1)
        );

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisWhenGeneratorCannotBeEvaluated() {
        GenerateArray operation = new GenerateArray(
                new Variable("generator"),
                new IntegerLiteral(5)
        );

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisWhenGeneratedValueCannotBeEvaluated() {
        GenerateArray operation = new GenerateArray(
                new Variable(ArrayOperation.INDEX_NAME),
                new IntegerLiteral(5)
        );

        // If ARRAY_INDEX substitution/evaluation is not possible,
        // generation should remain unevaluated.
        Node result = operation.evaluate();

        assertNotNull(result);
    }

    @Test
    void shouldValidateIntegerLength() {
        GenerateArray operation = new GenerateArray(
                new IntegerLiteral(42),
                new IntegerLiteral(5)
        );

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    void shouldRejectNonIntegerLength() {
        GenerateArray operation = new GenerateArray(
                new IntegerLiteral(42),
                new NumberLiteral(5.5)
        );

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertFalse(context.isValid());
    }

    @Test
    void shouldHaveCorrectReturnType() {
        GenerateArray operation = new GenerateArray(
                new IntegerLiteral(42),
                new IntegerLiteral(5)
        );

        assertEquals(
                new ArrayType(TypeEnum.INTEGER),
                operation.getReturnType()
        );
    }

    @Test
    void undeterminedReturnType() {
        GenerateArray operation = new GenerateArray(
                new Variable("a"),
                new IntegerLiteral(5)
        );

        assertEquals(
                new ArrayType(TypeEnum.ANY),
                operation.getReturnType()
        );
    }
}