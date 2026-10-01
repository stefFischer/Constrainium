package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.AdditionOperator;
import at.sfischer.constraints.model.operators.numbers.MultiplicationOperator;
import at.sfischer.constraints.model.operators.numbers.SubtractionOperator;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CombineArraysTest {

    @Test
    void shouldCombineTwoArraysUsingAddition() {
        ArrayValues<?> left = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2),
                        new IntegerLiteral(3)
                }
        );
        ArrayValues<?> right = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(10),
                        new IntegerLiteral(20),
                        new IntegerLiteral(30)
                }
        );
        Node operator = new AdditionOperator(
                new Variable(ArrayOperation.LEFT_ELEMENT_NAME),
                new Variable(ArrayOperation.RIGHT_ELEMENT_NAME)
        );
        CombineArrays operation = new CombineArrays(left, right, operator);

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(3, array.getValue().length);
        assertEquals(11.0, ((NumberLiteral) array.getValue()[0]).getValue());
        assertEquals(22.0, ((NumberLiteral) array.getValue()[1]).getValue());
        assertEquals(33.0, ((NumberLiteral) array.getValue()[2]).getValue());
    }

    @Test
    void shouldCombineArraysUsingSubtraction() {
        ArrayValues<?> left = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(10),
                        new IntegerLiteral(20),
                        new IntegerLiteral(30)
                }
        );
        ArrayValues<?> right = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2),
                        new IntegerLiteral(3)
                }
        );
        Node operator = new SubtractionOperator(
                new Variable(ArrayOperation.LEFT_ELEMENT_NAME),
                new Variable(ArrayOperation.RIGHT_ELEMENT_NAME)
        );
        CombineArrays operation = new CombineArrays(left, right, operator);

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(9.0, ((NumberLiteral) array.getValue()[0]).getValue());
        assertEquals(18.0, ((NumberLiteral) array.getValue()[1]).getValue());
        assertEquals(27.0, ((NumberLiteral) array.getValue()[2]).getValue());
    }

    @Test
    void shouldCombineArraysUsingMultiplication() {
        ArrayValues<?> left = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(2),
                        new IntegerLiteral(3),
                        new IntegerLiteral(4)
                }
        );
        ArrayValues<?> right = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(5),
                        new IntegerLiteral(6),
                        new IntegerLiteral(7)
                }
        );
        Node operator = new MultiplicationOperator(
                new Variable(ArrayOperation.LEFT_ELEMENT_NAME),
                new Variable(ArrayOperation.RIGHT_ELEMENT_NAME)
        );
        CombineArrays operation = new CombineArrays(left, right, operator);

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(10.0, ((NumberLiteral) array.getValue()[0]).getValue());
        assertEquals(18.0, ((NumberLiteral) array.getValue()[1]).getValue());
        assertEquals(28.0, ((NumberLiteral) array.getValue()[2]).getValue());
    }

    @Test
    void shouldCombineNumberArrays() {
        ArrayValues<?> left = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1.5),
                        new NumberLiteral(2.5),
                        new NumberLiteral(3.5)
                }
        );
        ArrayValues<?> right = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(0.5),
                        new NumberLiteral(1.5),
                        new NumberLiteral(2.5)
                }
        );
        Node operator = new AdditionOperator(
                new Variable(ArrayOperation.LEFT_ELEMENT_NAME),
                new Variable(ArrayOperation.RIGHT_ELEMENT_NAME)
        );
        CombineArrays operation = new CombineArrays(left, right, operator);

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(2.0, ((NumberLiteral) array.getValue()[0]).getValue().doubleValue(), 1e-10);
        assertEquals(4.0, ((NumberLiteral) array.getValue()[1]).getValue().doubleValue(), 1e-10);
        assertEquals(6.0, ((NumberLiteral) array.getValue()[2]).getValue().doubleValue(), 1e-10);
    }

    @Test
    void shouldUseArrayIndexInOperator() {
        ArrayValues<?> left = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(10),
                        new IntegerLiteral(10),
                        new IntegerLiteral(10),
                        new IntegerLiteral(10)
                }
        );
        ArrayValues<?> right = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(1),
                        new IntegerLiteral(1),
                        new IntegerLiteral(1)
                }
        );
        Node operator = new AdditionOperator(
                new AdditionOperator(
                        new Variable(ArrayOperation.LEFT_ELEMENT_NAME),
                        new Variable(ArrayOperation.RIGHT_ELEMENT_NAME)
                ),
                new Variable(ArrayOperation.INDEX_NAME)
        );
        CombineArrays operation = new CombineArrays(left, right, operator);

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(11.0, ((NumberLiteral) array.getValue()[0]).getValue());
        assertEquals(12.0, ((NumberLiteral) array.getValue()[1]).getValue());
        assertEquals(13.0, ((NumberLiteral) array.getValue()[2]).getValue());
        assertEquals(14.0, ((NumberLiteral) array.getValue()[3]).getValue());
    }

    @Test
    void shouldUseArrayIndexWithoutUsingElements() {
        ArrayValues<?> left = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(100),
                        new IntegerLiteral(100),
                        new IntegerLiteral(100)
                }
        );
        ArrayValues<?> right = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(200),
                        new IntegerLiteral(200),
                        new IntegerLiteral(200)
                }
        );
        Node operator = new Variable(ArrayOperation.INDEX_NAME);
        CombineArrays operation = new CombineArrays(left, right, operator);

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(0, ((IntegerLiteral) array.getValue()[0]).getValue());
        assertEquals(1, ((IntegerLiteral) array.getValue()[1]).getValue());
        assertEquals(2, ((IntegerLiteral) array.getValue()[2]).getValue());
    }

    @Test
    void shouldCombineMoreThanTwoArraysByNesting() {
        ArrayValues<?> first = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2),
                        new IntegerLiteral(3)
                }
        );
        ArrayValues<?> second = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(10),
                        new IntegerLiteral(20),
                        new IntegerLiteral(30)
                }
        );
        ArrayValues<?> third = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(100),
                        new IntegerLiteral(200),
                        new IntegerLiteral(300)
                }
        );
        ArrayValues<?> fourth = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1000),
                        new IntegerLiteral(2000),
                        new IntegerLiteral(3000)
                }
        );
        Variable left = new Variable(ArrayOperation.LEFT_ELEMENT_NAME);
        Variable right = new Variable(ArrayOperation.RIGHT_ELEMENT_NAME);
        CombineArrays firstTwo = new CombineArrays(first, second, new AdditionOperator(left, right));
        CombineArrays firstThree = new CombineArrays(firstTwo, third, new AdditionOperator(left, right));
        CombineArrays allFour = new CombineArrays(firstThree, fourth, new AdditionOperator(left, right));

        Node result = allFour.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(1111.0, ((NumberLiteral) array.getValue()[0]).getValue());
        assertEquals(2222.0, ((NumberLiteral) array.getValue()[1]).getValue());
        assertEquals(3333.0, ((NumberLiteral) array.getValue()[2]).getValue());
    }

    @Test
    void shouldCombineMultipleArraysWithIndexDependentFinalOperation() {
        ArrayValues<?> first = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2),
                        new IntegerLiteral(3)
                }
        );
        ArrayValues<?> second = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(10),
                        new IntegerLiteral(20),
                        new IntegerLiteral(30)
                }
        );
        ArrayValues<?> third = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(100),
                        new IntegerLiteral(200),
                        new IntegerLiteral(300)
                }
        );
        Variable left = new Variable(ArrayOperation.LEFT_ELEMENT_NAME);
        Variable right = new Variable(ArrayOperation.RIGHT_ELEMENT_NAME);
        CombineArrays firstCombination = new CombineArrays(first, second, new AdditionOperator(left, right));
        Node finalOperator = new AdditionOperator(new AdditionOperator(left, right), new Variable(ArrayOperation.INDEX_NAME));
        CombineArrays operation = new CombineArrays(firstCombination, third, finalOperator);

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> array = (ArrayValues<?>) result;
        assertEquals(111.0, ((NumberLiteral) array.getValue()[0]).getValue());
        assertEquals(223.0, ((NumberLiteral) array.getValue()[1]).getValue());
        assertEquals(335.0, ((NumberLiteral) array.getValue()[2]).getValue());
    }

    @Test
    void shouldReturnThisForDifferentArrayLengths() {
        ArrayValues<?> left = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                }
        );
        ArrayValues<?> right = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(10)
                }
        );
        Node operator = new AdditionOperator(
                new Variable(ArrayOperation.LEFT_ELEMENT_NAME),
                new Variable(ArrayOperation.RIGHT_ELEMENT_NAME)
        );
        CombineArrays operation = new CombineArrays(left, right, operator);

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisWhenLeftArrayCannotBeEvaluated() {
        Node operator = new AdditionOperator(
                new Variable(ArrayOperation.LEFT_ELEMENT_NAME),
                new Variable(ArrayOperation.RIGHT_ELEMENT_NAME)
        );
        CombineArrays operation = new CombineArrays(
                        new Variable("left"),
                        new ArrayValues<>(
                                TypeEnum.INTEGER,
                                new Value<?>[] {
                                        new IntegerLiteral(1)
                                }
                        ),
                        operator
                );

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisWhenRightArrayCannotBeEvaluated() {
        Node operator = new AdditionOperator(
                new Variable(ArrayOperation.LEFT_ELEMENT_NAME),
                new Variable(ArrayOperation.RIGHT_ELEMENT_NAME)
        );
        CombineArrays operation = new CombineArrays(
                        new ArrayValues<>(
                                TypeEnum.INTEGER,
                                new Value<?>[] {
                                        new IntegerLiteral(1)
                                }
                        ),
                        new Variable("right"),
                        operator
                );

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldReturnThisWhenOperatorCannotBeEvaluated() {
        ArrayValues<?> left = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1)
                }
        );
        ArrayValues<?> right = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(2)
                }
        );
        CombineArrays operation = new CombineArrays(
                        left,
                        right,
                        new Variable("operator")
                );

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldValidateArraysOfEqualLength() {
        ArrayValues<?> left = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                }
        );
        ArrayValues<?> right = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(3),
                        new IntegerLiteral(4)
                }
        );
        Node operator = new AdditionOperator(
                new Variable(ArrayOperation.LEFT_ELEMENT_NAME),
                new Variable(ArrayOperation.RIGHT_ELEMENT_NAME)
        );

        CombineArrays operation = new CombineArrays(left, right, operator);
        ValidationContext context = new ValidationContext();

        operation.validate(context);

        assertTrue(context.isValid());
    }

    @Test
    void shouldRejectArraysOfDifferentLengths() {
        ArrayValues<?> left = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                }
        );
        ArrayValues<?> right = new ArrayValues<>(
                TypeEnum.INTEGER,
                new Value<?>[] {
                        new IntegerLiteral(3)
                }
        );
        Node operator = new AdditionOperator(
                new Variable(ArrayOperation.LEFT_ELEMENT_NAME),
                new Variable(ArrayOperation.RIGHT_ELEMENT_NAME)
        );
        CombineArrays operation = new CombineArrays(left, right, operator);

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertFalse(context.isValid());
    }

    @Test
    void shouldHaveCorrectReturnType() {
        Node operator = new AdditionOperator(
                new Variable(ArrayOperation.LEFT_ELEMENT_NAME),
                new Variable(ArrayOperation.RIGHT_ELEMENT_NAME)
        );
        CombineArrays operation =
                new CombineArrays(
                        new Variable("left"),
                        new Variable("right"),
                        operator
                );

        assertEquals(
                new ArrayType(TypeEnum.NUMBER),
                operation.getReturnType()
        );
    }
}
