package at.sfischer.constraints.model.operators.numbers;

import at.sfischer.constraints.model.IntegerLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class RoundTest {

    @Test
    public void simpleEvaluateTest() {
        Node num = new NumberLiteral(5.4);
        Round operator = new Round(num);
        Node result = operator.evaluate();

        assertInstanceOf(NumberLiteral.class,result);
        assertEquals(5.0, ((NumberLiteral)result).getValue());
    }
}
