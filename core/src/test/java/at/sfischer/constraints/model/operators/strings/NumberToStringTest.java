package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NumberToStringTest {

    @Test
    public void convertsNumbersPreservingTheirRepresentation() {
        for (Number number : new Number[]{0, -42, 42L, 3.14, new BigDecimal("123.4500")}) {
            Node result = new NumberToString(new NumberLiteral(number)).evaluate();
            assertEquals(number.toString(), assertInstanceOf(StringLiteral.class, result).getValue());
        }
        assertEquals(new StringLiteral("42"), new NumberToString(new IntegerLiteral(42)).evaluate());
    }

    @Test
    public void defersUnresolvedAndNonNumericInputs() {
        for (Node input : new Node[]{new Variable("value"), new StringLiteral("42"), BooleanLiteral.TRUE}) {
            NumberToString operator = new NumberToString(input);
            assertSame(operator, operator.evaluate());
        }
    }

    @Test
    public void substitutesVariablesWithoutChangingOriginal() {
        Variable value = new Variable("value");
        NumberToString operator = new NumberToString(value);
        Node substituted = operator.setVariableValues(Map.of(value, new NumberLiteral(12.5)));

        assertInstanceOf(NumberToString.class, substituted);
        assertEquals(new StringLiteral("12.5"), substituted.evaluate());
        assertSame(operator, operator.evaluate());
    }

    @Test
    public void composesWithOtherOperators() {
        Node result = new StringConcat(new StringLiteral("Length: "),
                new NumberToString(new StringLength(new StringLiteral("hello")))).evaluate();

        assertEquals(new StringLiteral("Length: 5"), result);
    }

    @Test
    public void validatesNumericInputsAndRejectsOtherTypes() {
        for (Node input : new Node[]{new NumberLiteral(1.5), new IntegerLiteral(1), new Variable("value")}) {
            ValidationContext context = new ValidationContext();
            new NumberToString(input).validate(context);
            assertTrue(context.isValid());
        }
        for (Node input : new Node[]{new StringLiteral("42"), BooleanLiteral.TRUE}) {
            ValidationContext context = new ValidationContext();
            new NumberToString(input).validate(context);
            assertFalse(context.isValid());
        }
    }

    @Test
    public void infersNumericInputAndClones() {
        Variable value = new Variable("value");
        NumberToString operator = new NumberToString(value);

        assertEquals(TypeEnum.STRING, operator.getReturnType());
        assertEquals(Map.of(value, TypeEnum.NUMBER), operator.inferVariableTypes());
        NumberToString clone = assertInstanceOf(NumberToString.class, operator.cloneNode());
        assertEquals(operator, clone);
        assertNotSame(value, clone.getParameter(0));
    }
}
