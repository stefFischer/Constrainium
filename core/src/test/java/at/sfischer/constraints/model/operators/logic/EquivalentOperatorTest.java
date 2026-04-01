package at.sfischer.constraints.model.operators.logic;

import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class EquivalentOperatorTest {

    @Test
    public void evaluateTrue() {
        EquivalentOperator operator = new EquivalentOperator(BooleanLiteral.TRUE, BooleanLiteral.TRUE);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(true, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateTrueBothFalse() {
        EquivalentOperator operator = new EquivalentOperator(BooleanLiteral.FALSE, BooleanLiteral.FALSE);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(true, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateFalse() {
        EquivalentOperator operator = new EquivalentOperator(BooleanLiteral.FALSE, BooleanLiteral.TRUE);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(false, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateSimplificationTest() {
        Node left = new Variable("a");
        Node right = new Variable("a");
        EquivalentOperator operator = new EquivalentOperator(left, right);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(true, ((BooleanLiteral)result).getValue());
    }
}
