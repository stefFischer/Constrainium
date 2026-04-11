package at.sfischer.constraints.model.operators.objects;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.ComplexValue;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ObjectEqualsTest {

    @Test
    public void evaluateTrue() {
        ComplexValue value1 = new ComplexValue(DataObject.parseData("{size:0, object:{number:2}, array:[1,2,3,4]}"));
        ComplexValue value2 = new ComplexValue(DataObject.parseData("{size:0, object:{number:2}, array:[1,2,3,4]}"));
        ObjectEquals operator = new ObjectEquals(value1, value2);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(true, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateFalse() {
        ComplexValue value1 = new ComplexValue(DataObject.parseData("{size:0, object:{number:2}, array:[1,2,3,4]}"));
        ComplexValue value2 = new ComplexValue(DataObject.parseData("{size:0, object:{number:2}, array:[1,2,3]}"));
        ObjectEquals operator = new ObjectEquals(value1, value2);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(false, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateSimplificationTest() {
        Node left = new Variable("a");
        Node right = new Variable("a");
        ObjectEquals operator = new ObjectEquals(left, right);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(true, ((BooleanLiteral)result).getValue());
    }
}
