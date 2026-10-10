package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;
import at.sfischer.constraints.model.validation.ValidationContext;
import at.sfischer.constraints.parser.registry.FunctionCreateException;
import at.sfischer.constraints.parser.registry.FunctionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AbsoluteDifferenceTest {

    @Test
    void createsFunctionFromDslDefinition() throws FunctionCreateException {
        Function function = FunctionRegistry.create(
                "absoluteDifference",
                List.of(
                        new IntegerLiteral(5),
                        new IntegerLiteral(2)
                )
        );

        Node result = function.evaluate();

        NumberLiteral value = assertInstanceOf(NumberLiteral.class, result);
        assertEquals(3, value.getValue().intValue());
    }

    @ParameterizedTest
    @CsvSource({
            "5, 2, 3",
            "2, 5, 3",
            "5, 5, 0",
            "0, 5, 5",
            "-5, 2, 7",
            "-5, -2, 3",
            "2, -5, 7"
    })
    void evaluatesAbsoluteDifference(int a, int b, int expected) throws FunctionCreateException {
        Function function = FunctionRegistry.create(
                "absoluteDifference",
                List.of(
                        new IntegerLiteral(a),
                        new IntegerLiteral(b)
                )
        );

        Node result = function.evaluate();

        NumberLiteral actual = assertInstanceOf(NumberLiteral.class, result);
        assertEquals(expected, actual.getValue().intValue());
    }

    @Test
    void rejectsTooFewArguments() {
        FunctionCreateException exception = assertThrows(
                FunctionCreateException.class,
                () -> FunctionRegistry.create(
                        "absoluteDifference",
                        List.of(new IntegerLiteral(5))
                )
        );
    }

    @Test
    void rejectsTooManyArguments() {
        assertThrows(
                FunctionCreateException.class,
                () -> FunctionRegistry.create(
                        "absoluteDifference",
                        List.of(
                                new IntegerLiteral(5),
                                new IntegerLiteral(2),
                                new IntegerLiteral(1)
                        )
                )
        );
    }

    @Test
    void rejectsNonNumericArguments() throws FunctionCreateException {
        Function function = FunctionRegistry.create(
                "absoluteDifference",
                List.of(
                        new StringLiteral("five"),
                        new IntegerLiteral(2)
                )
        );

        ValidationContext context = new ValidationContext();
        function.validate(context);

        assertFalse(context.isValid());
    }

    @Test
    void canBeUsedInsideAnotherExpression() throws FunctionCreateException {
        Node expression = new GreaterThanOperator(
                FunctionRegistry.create(
                        "absoluteDifference",
                        List.of(
                                new IntegerLiteral(5),
                                new IntegerLiteral(2)
                        )
                ),
                new IntegerLiteral(0)
        );

        Node result = expression.evaluate();

        BooleanLiteral actual = assertInstanceOf(BooleanLiteral.class, result);
        assertTrue(actual.getValue());
    }
}
