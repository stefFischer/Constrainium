package at.sfischer.constraints.parser;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.numbers.AdditionOperator;
import at.sfischer.constraints.model.operators.numbers.SubtractionOperator;
import at.sfischer.constraints.model.validation.ValidationContext;
import at.sfischer.constraints.model.validation.ValidationMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserFunctionTest {

    @Test
    void evaluateReturnsEvaluatedFunctionBody() {
        UserFunction function = new UserFunction(
                "difference",
                List.of(new IntegerLiteral(5), new IntegerLiteral(2)),
                List.of("a", "b"),
                List.of(TypeEnum.NUMBER, TypeEnum.NUMBER),
                new SubtractionOperator(
                        new IntegerLiteral(5),
                        new IntegerLiteral(2)
                )
        );

        Node result = function.evaluate();

        NumberLiteral number = assertInstanceOf(NumberLiteral.class, result);
        assertEquals(3, number.getValue().intValue());
    }

    @Test
    void evaluateReturnsThisWhenBodyCannotBeEvaluated() {
        Node body = mock(Node.class);

        when(body.setVariableNameValues(anyMap())).thenReturn(body);
        when(body.evaluate()).thenReturn(body);

        UserFunction function = new UserFunction(
                "test",
                List.of(),
                List.of(),
                List.of(),
                body
        );

        Node result = function.evaluate();

        assertSame(function, result);
    }

    @Test
    void evaluateEvaluatesFunctionBody() {
        Node body = mock(Node.class);
        Node evaluated = new IntegerLiteral(42);

        when(body.setVariableNameValues(anyMap())).thenReturn(body);
        when(body.evaluate()).thenReturn(evaluated);

        UserFunction function = new UserFunction(
                "test",
                List.of(),
                List.of(),
                List.of(),
                body
        );

        assertSame(evaluated, function.evaluate());

        verify(body).evaluate();
    }

    @Test
    void getReturnTypeReturnsFunctionBodyReturnType() {
        Node body = mock(Node.class);

        when(body.getReturnType()).thenReturn(TypeEnum.NUMBER);

        UserFunction function = new UserFunction(
                "test",
                List.of(),
                List.of(),
                List.of(),
                body
        );

        assertEquals(TypeEnum.NUMBER, function.getReturnType());

        verify(body).getReturnType();
    }

    @Test
    void getReturnTypeReturnsBodyType() {
        Node body = new AdditionOperator(
                new IntegerLiteral(1),
                new IntegerLiteral(2)
        );

        UserFunction function = new UserFunction(
                "test",
                List.of(),
                List.of(),
                List.of(),
                body
        );

        assertEquals(TypeEnum.NUMBER, function.getReturnType());
    }

    @Test
    void parameterTypesReturnsDeclaredParameterTypes() {
        List<Type> parameterTypes = List.of(
                TypeEnum.NUMBER,
                TypeEnum.STRING
        );

        UserFunction function = new UserFunction(
                "test",
                List.of(),
                List.of(),
                parameterTypes,
                new IntegerLiteral(42)
        );

        assertEquals(parameterTypes, function.parameterTypes());
    }

    @Test
    void getChildrenReturnsFunctionArguments() {
        List<Node> arguments = List.of(
                new IntegerLiteral(5),
                new IntegerLiteral(2)
        );

        UserFunction function = new UserFunction(
                "difference",
                arguments,
                List.of("a", "b"),
                List.of(TypeEnum.NUMBER, TypeEnum.NUMBER),
                new SubtractionOperator(
                        new IntegerLiteral(5),
                        new IntegerLiteral(2)
                )
        );

        assertEquals(arguments, function.getChildren());
    }

    @Test
    void setVariableValuesReplacesVariablesInFunctionBody() {
        Variable a = new Variable("a");
        UserFunction function = new UserFunction(
                "identity",
                List.of(a),
                List.of("a"),
                List.of(TypeEnum.NUMBER),
                a
        );

        UserFunction result = assertInstanceOf(
                UserFunction.class,
                function.setVariableValue(a, new IntegerLiteral(42))
        );

        assertEquals(
                new IntegerLiteral(42),
                result.evaluate()
        );
    }

    @Test
    void setVariableValuesReplacesVariablesInArguments() {
        Variable a = new Variable("a");

        UserFunction function = new UserFunction(
                "identity",
                List.of(a),
                List.of("a"),
                List.of(TypeEnum.NUMBER),
                new IntegerLiteral(42)
        );

        UserFunction result = assertInstanceOf(
                UserFunction.class,
                function.setVariableValues(
                        Map.of(a, new IntegerLiteral(10))
                )
        );

        assertEquals(
                List.of(new IntegerLiteral(10)),
                result.getChildren()
        );
    }

    @Test
    void setVariableValuesReplacesVariablesInBodyAndArguments() {
        Variable a = new Variable("a");
        Variable b = new Variable("b");

        UserFunction function = new UserFunction(
                "difference",
                List.of(a, b),
                List.of("a", "b"),
                List.of(TypeEnum.NUMBER, TypeEnum.NUMBER),
                new SubtractionOperator(a, b)
        );

        UserFunction result = assertInstanceOf(
                UserFunction.class,
                function.setVariableValues(
                        Map.of(
                                a, new IntegerLiteral(10),
                                b, new IntegerLiteral(3)
                        )
                )
        );

        assertEquals(
                List.of(
                        new IntegerLiteral(10),
                        new IntegerLiteral(3)
                ),
                result.getChildren()
        );

        assertEquals(
                new NumberLiteral(7.0),
                result.evaluate()
        );
    }

    @Test
    void setVariableValuesDoesNotModifyOriginalFunction() {
        Variable a = new Variable("a");

        UserFunction function = new UserFunction(
                "identity",
                List.of(a),
                List.of("a"),
                List.of(TypeEnum.NUMBER),
                a
        );

        UserFunction result = assertInstanceOf(
                UserFunction.class,
                function.setVariableValues(
                        Map.of(a, new IntegerLiteral(42))
                )
        );

        assertNotSame(function, result);

        assertEquals(
                List.of(a),
                function.getChildren()
        );

        assertEquals(
                List.of(new IntegerLiteral(42)),
                result.getChildren()
        );
    }

    private static ValidationContext validate(UserFunction function) {
        ValidationContext context = new ValidationContext();
        function.validate(context);
        return context;
    }

    @Test
    void validatesCorrectNumberAndTypesOfParameters() {
        UserFunction function = new UserFunction(
                "test",
                List.of(
                        new IntegerLiteral(5),
                        new StringLiteral("hello")
                ),
                List.of("a", "b"),
                List.of(
                        TypeEnum.NUMBER,
                        TypeEnum.STRING
                ),
                BooleanLiteral.TRUE
        );

        ValidationContext context = validate(function);

        assertTrue(context.isValid());
    }

    @Test
    void rejectsTooFewParameters() {
        UserFunction function = new UserFunction(
                "test",
                List.of(new IntegerLiteral(5)),
                List.of("a", "b"),
                List.of(
                        TypeEnum.NUMBER,
                        TypeEnum.STRING
                ),
                BooleanLiteral.TRUE
        );

        ValidationContext context = validate(function);

        assertFalse(context.isValid());
        assertTrue(context.getMessages().stream()
                .anyMatch(message -> message.message().contains("Wrong number of parameters.")));
    }

    @Test
    void rejectsTooManyParameters() {
        UserFunction function = new UserFunction(
                "test",
                List.of(
                        new IntegerLiteral(5),
                        new StringLiteral("hello"),
                        new IntegerLiteral(10)
                ),
                List.of("a", "b"),
                List.of(
                        TypeEnum.NUMBER,
                        TypeEnum.STRING
                ),
                BooleanLiteral.TRUE
        );

        ValidationContext context = validate(function);

        assertFalse(context.isValid());
        assertTrue(context.getMessages().stream()
                .anyMatch(message -> message.message().contains("Wrong number of parameters.")));
    }

    @Test
    void acceptsCompatibleScalarParameterTypes() {
        UserFunction function = new UserFunction(
                "test",
                List.of(new IntegerLiteral(5)),
                List.of("a"),
                List.of(TypeEnum.NUMBER),
                BooleanLiteral.TRUE
        );

        ValidationContext context = validate(function);

        assertTrue(context.isValid());
    }

    @Test
    void rejectsIncompatibleScalarParameterType() {
        UserFunction function = new UserFunction(
                "test",
                List.of(new StringLiteral("hello")),
                List.of("a"),
                List.of(TypeEnum.NUMBER),
                BooleanLiteral.TRUE
        );

        ValidationContext context = validate(function);

        assertFalse(context.isValid());

        assertTrue(context.getMessages().stream()
                .anyMatch(message ->
                        message.message().contains("Wrong parameter type at index 0.")));
    }

    @Test
    void parameterTypeErrorIsAssociatedWithUserFunction() {
        UserFunction function = new UserFunction(
                "test",
                List.of(new StringLiteral("hello")),
                List.of("a"),
                List.of(TypeEnum.NUMBER),
                BooleanLiteral.TRUE
        );

        ValidationContext context = validate(function);

        assertFalse(context.isValid());

        assertTrue(context.getMessages().stream()
                .anyMatch(message -> message.node() == function));
    }

    @Test
    void acceptsMatchingArrayParameterType() {
        ArrayValues<NumberLiteral> argument = new ArrayValues<>(
                TypeEnum.NUMBER,
                new NumberLiteral[] {
                        new IntegerLiteral(1),
                        new IntegerLiteral(2)
                }
        );

        UserFunction function = new UserFunction(
                "test",
                List.of(argument),
                List.of("a"),
                List.of(new ArrayType(TypeEnum.NUMBER)),
                BooleanLiteral.TRUE
        );

        ValidationContext context = validate(function);

        for (ValidationMessage message : context.getMessages()) {
            System.out.println(message);
        }

        assertTrue(context.isValid());
    }

    @Test
    void rejectsScalarForArrayParameter() {
        UserFunction function = new UserFunction(
                "test",
                List.of(new IntegerLiteral(5)),
                List.of("a"),
                List.of(new ArrayType(TypeEnum.NUMBER)),
                BooleanLiteral.TRUE
        );

        ValidationContext context = validate(function);

        assertFalse(context.isValid());

        assertTrue(context.getMessages().stream()
                .anyMatch(message ->
                        message.message().contains("Wrong parameter type at index 0.")));
    }

    @Test
    void rejectsArrayWithWrongElementType() {
        ArrayValues<StringLiteral> argument = new ArrayValues<>(
                TypeEnum.STRING,
                new StringLiteral[] {
                        new StringLiteral("a"),
                        new StringLiteral("b")
                }
        );

        UserFunction function = new UserFunction(
                "test",
                List.of(argument),
                List.of("a"),
                List.of(new ArrayType(TypeEnum.NUMBER)),
                BooleanLiteral.TRUE
        );

        ValidationContext context = validate(function);

        assertFalse(context.isValid());

        assertTrue(context.getMessages().stream()
                .anyMatch(message ->
                        message.message().contains(
                                "Array element type does not match parameter element type at index 0."
                        )));
    }

    @Test
    void acceptsAnyParameterType() {
        UserFunction function = new UserFunction(
                "test",
                List.of(new StringLiteral("hello")),
                List.of("a"),
                List.of(TypeEnum.ANY),
                BooleanLiteral.TRUE
        );

        ValidationContext context = validate(function);

        assertTrue(context.isValid());
    }

    @Test
    void acceptsArrayWithAnyElementType() {
        ArrayValues<StringLiteral> argument = new ArrayValues<>(
                TypeEnum.STRING,
                new StringLiteral[] {
                        new StringLiteral("a")
                }
        );

        UserFunction function = new UserFunction(
                "test",
                List.of(argument),
                List.of("a"),
                List.of(new ArrayType(TypeEnum.ANY)),
                BooleanLiteral.TRUE
        );

        ValidationContext context = validate(function);

        assertTrue(context.isValid());
    }

    @Test
    void propagatesValidationErrorsFromParameters() {
        UserFunction invalidArgument = new UserFunction(
                "inner",
                List.of(new StringLiteral("not a number")),
                List.of("a"),
                List.of(TypeEnum.NUMBER),
                BooleanLiteral.TRUE
        );

        UserFunction function = new UserFunction(
                "outer",
                List.of(invalidArgument),
                List.of("a"),
                List.of(TypeEnum.BOOLEAN),
                BooleanLiteral.TRUE
        );

        ValidationContext context = validate(function);

        assertFalse(context.isValid());
        assertFalse(context.getMessages().isEmpty());
    }
}
