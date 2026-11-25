package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StringConcatTest {

    @Test
    public void concatenatesInArgumentOrder() {
        Node result = new StringConcat(new StringLiteral("Hello "), new StringLiteral("World")).evaluate();

        assertEquals("Hello World", assertInstanceOf(StringLiteral.class, result).getValue());
    }

    @Test
    public void supportsEmptyStrings() {
        for (String first : new String[]{"", "hello"}) {
            for (String second : new String[]{"", "world"}) {
                Node result = new StringConcat(new StringLiteral(first), new StringLiteral(second)).evaluate();
                assertEquals(first + second, assertInstanceOf(StringLiteral.class, result).getValue());
            }
        }
    }

    @Test
    public void defersUnresolvedArguments() {
        StringConcat unresolvedFirst = new StringConcat(new Variable("first"), new StringLiteral("world"));
        StringConcat unresolvedSecond = new StringConcat(new StringLiteral("hello"), new Variable("second"));

        assertSame(unresolvedFirst, unresolvedFirst.evaluate());
        assertSame(unresolvedSecond, unresolvedSecond.evaluate());
    }

    @Test
    public void substitutesBothVariablesWithoutChangingOriginal() {
        Variable first = new Variable("first");
        Variable second = new Variable("second");
        StringConcat operator = new StringConcat(first, second);
        Node substituted = operator.setVariableValues(Map.of(
                first, new StringLiteral("hello"), second, new StringLiteral("world")));

        assertInstanceOf(StringConcat.class, substituted);
        assertEquals(new StringLiteral("helloworld"), substituted.evaluate());
        assertSame(operator, operator.evaluate());
    }

    @Test
    public void evaluatesNestedConcatenation() {
        Node result = new StringConcat(
                new StringConcat(new StringLiteral("a"), new StringLiteral("b")),
                new StringLiteral("c")).evaluate();

        assertEquals(new StringLiteral("abc"), result);
    }

    @Test
    public void usesSharedStringArgumentConversions() {
        Node result = new StringConcat(new NumberLiteral(42), BooleanLiteral.TRUE).evaluate();

        assertEquals(new StringLiteral("42true"), result);
    }

    @Test
    public void validatesStringsAndRejectsArrayArguments() {
        ValidationContext valid = new ValidationContext();
        new StringConcat(new StringLiteral("a"), new StringLiteral("b")).validate(valid);
        assertTrue(valid.isValid());

        ValidationContext invalid = new ValidationContext();
        new StringConcat(new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{}),
                new StringLiteral("b")).validate(invalid);
        assertFalse(invalid.isValid());
    }

    @Test
    public void infersStringVariablesAndClonesNestedExpressions() {
        Variable first = new Variable("first");
        Variable second = new Variable("second");
        StringConcat nested = new StringConcat(first, new StringLiteral("!"));
        StringConcat operator = new StringConcat(nested, second);

        assertEquals(TypeEnum.STRING, operator.getReturnType());
        assertEquals(Map.of(first, TypeEnum.STRING, second, TypeEnum.STRING), operator.inferVariableTypes());
        StringConcat clone = assertInstanceOf(StringConcat.class, operator.cloneNode());
        assertEquals(operator, clone);
        assertNotSame(nested, clone.getParameter(0));
    }
}
