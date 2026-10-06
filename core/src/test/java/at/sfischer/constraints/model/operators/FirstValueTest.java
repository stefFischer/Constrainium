package at.sfischer.constraints.model.operators;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FirstValueTest {

    @Test
    void shouldReturnFirstValue() {
        Node operation = new FirstValue(
                new IntegerLiteral(10),
                new IntegerLiteral(20)
        );

        Node result = operation.evaluate();

        IntegerLiteral integerLiteral = assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(10, integerLiteral.getValue());
    }

    @Test
    void shouldReturnSecondValueWhenFirstIsNotEvaluable() {
        Variable variable = new Variable("x");
        Node operation = new FirstValue(
                variable,
                new IntegerLiteral(20)
        );

        Node result = operation.evaluate();

        IntegerLiteral integerLiteral = assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(20, integerLiteral.getValue());
    }

    @Test
    void shouldReturnThisWhenNeitherParameterIsEvaluable() {
        Variable first = new Variable("x");
        Variable second = new Variable("y");
        Node operation = new FirstValue(first, second);

        Node result = operation.evaluate();

        assertSame(operation, result);
    }

    @Test
    void shouldPreferFirstValueEvenWhenBothAreValues() {
        Node operation = new FirstValue(
                new IntegerLiteral(10),
                new IntegerLiteral(20)
        );

        Node result = operation.evaluate();

        IntegerLiteral integerLiteral = assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(10, integerLiteral.getValue());
    }

    @Test
    void shouldSupportDifferentValueTypes() {
        Node operation = new FirstValue(
                new IntegerLiteral(10),
                new StringLiteral("fallback")
        );

        Node result = operation.evaluate();

        IntegerLiteral integerLiteral = assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(10, integerLiteral.getValue());
    }

    @Test
    void shouldReturnSecondValueOfDifferentTypeWhenFirstIsNotEvaluable() {
        Node operation = new FirstValue(
                new Variable("x"),
                new StringLiteral("fallback")
        );

        Node result = operation.evaluate();

        StringLiteral stringLiteral = assertInstanceOf(StringLiteral.class, result);
        assertEquals("fallback", stringLiteral.getValue());
    }

    @Test
    void shouldReturnCommonTypeWhenParameterTypesAreEqual() {
        FirstValue operation = new FirstValue(
                new IntegerLiteral(10),
                new IntegerLiteral(20)
        );

        assertEquals(
                TypeEnum.INTEGER,
                operation.getReturnType()
        );
    }

    @Test
    void shouldReturnAnyWhenParameterTypesDiffer() {
        FirstValue operation = new FirstValue(
                new IntegerLiteral(10),
                new NumberLiteral(20.0)
        );

        assertEquals(
                TypeEnum.ANY,
                operation.getReturnType()
        );
    }

    @Test
    void shouldReturnAnyWhenBothParameterTypesAreAny() {
        FirstValue operation = new FirstValue(
                new Variable("x"),
                new Variable("y")
        );

        assertEquals(
                TypeEnum.ANY,
                operation.getReturnType()
        );
    }

    @Test
    void shouldSubstituteVariableValues() {
        Variable variable = new Variable("x");
        FirstValue operation = new FirstValue(
                variable,
                new IntegerLiteral(20)
        );

        Node substituted = operation.setVariableValue(variable, new IntegerLiteral(42));

        Node result = substituted.evaluate();

        IntegerLiteral integerLiteral = assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(42, integerLiteral.getValue());
    }

    @Test
    void shouldUseSecondValueAfterVariableSubstitution() {
        Variable first = new Variable("x");
        Variable second = new Variable("y");
        FirstValue operation = new FirstValue(first, second);

        Node substituted = operation.setVariableValue(second, new IntegerLiteral(42));

        Node result = substituted.evaluate();

        IntegerLiteral integerLiteral = assertInstanceOf(IntegerLiteral.class, result);
        assertEquals(42, integerLiteral.getValue());
    }

    @Test
    void shouldPreserveUnresolvedFirstParameter() {
        Variable first = new Variable("x");
        FirstValue operation = new FirstValue(
                first,
                new IntegerLiteral(42)
        );

        Node substituted = operation.setVariableValues(Map.of());

        IntegerLiteral integerLiteral = assertInstanceOf(IntegerLiteral.class, substituted.evaluate());
        assertEquals(42, integerLiteral.getValue());
    }
}