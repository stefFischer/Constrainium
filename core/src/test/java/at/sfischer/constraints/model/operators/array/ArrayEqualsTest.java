package at.sfischer.constraints.model.operators.array;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ArrayEqualsTest {

    @Test
    public void evaluateTrueWithNumbers() {
        Node array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(-1),
                new NumberLiteral(3),
                new NumberLiteral(1)
        });
        Node array2 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(-1),
                new NumberLiteral(3),
                new NumberLiteral(1)
        });

        ArrayEquals operator = new ArrayEquals(array1, array2);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(true, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateFalseWithNumbers() {
        Node array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(-1),
                new NumberLiteral(3),
                new NumberLiteral(1)
        });
        Node array2 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(1),
                new NumberLiteral(3),
                new NumberLiteral(1)
        });

        ArrayEquals operator = new ArrayEquals(array1, array2);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(false, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateFalseWithNumbersWrongOrder() {
        Node array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(2),
                new NumberLiteral(3)
        });
        Node array2 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(3),
                new NumberLiteral(2)
        });

        ArrayEquals operator = new ArrayEquals(array1, array2);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(false, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateTrueWithStrings() {
        Node array1 = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("first"),
                new StringLiteral("second"),
                new StringLiteral("third")
        });
        Node array2 = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("first"),
                new StringLiteral("second"),
                new StringLiteral("third")
        });

        ArrayEquals operator = new ArrayEquals(array1, array2);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(true, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateFalseWithStrings() {
        Node array1 = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("first"),
                new StringLiteral("second"),
                new StringLiteral("third")
        });
        Node array2 = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("first"),
                new StringLiteral("third")
        });

        ArrayEquals operator = new ArrayEquals(array1, array2);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(false, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateTrueWithObjects() {
        Node array1 = new ArrayValues<>(TypeEnum.COMPLEXTYPE, new ComplexValue[]{
                new ComplexValue(DataObject.parseData("{size:0, object:{number:2}, array:[1,2,3,4]}")),
                new ComplexValue(DataObject.parseData("{size:0, object:{number:2}, array:[1]}"))
        });
        Node array2 = new ArrayValues<>(TypeEnum.COMPLEXTYPE, new ComplexValue[]{
                new ComplexValue(DataObject.parseData("{size:0, object:{number:2}, array:[1,2,3,4]}")),
                new ComplexValue(DataObject.parseData("{size:0, object:{number:2}, array:[1]}"))
        });

        ArrayEquals operator = new ArrayEquals(array1, array2);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(true, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateFalseWithObjects() {
        Node array1 = new ArrayValues<>(TypeEnum.COMPLEXTYPE, new ComplexValue[]{
                new ComplexValue(DataObject.parseData("{size:0, object:{number:2}, array:[1,2,3,4]}")),
                new ComplexValue(DataObject.parseData("{size:0, object:{number:2}, array:[1.2]}"))
        });
        Node array2 = new ArrayValues<>(TypeEnum.COMPLEXTYPE, new ComplexValue[]{
                new ComplexValue(DataObject.parseData("{size:0, object:{number:2}, array:[1,2,3,4]}")),
                new ComplexValue(DataObject.parseData("{size:0, object:{number:2}, array:[1]}"))
        });

        ArrayEquals operator = new ArrayEquals(array1, array2);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(false, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateFalseWithDifferentTypes() {
        Node array1 = new ArrayValues<>(TypeEnum.NUMBER, new NumberLiteral[]{
                new NumberLiteral(1),
                new NumberLiteral(3)
        });
        Node array2 = new ArrayValues<>(TypeEnum.STRING, new StringLiteral[]{
                new StringLiteral("first"),
                new StringLiteral("third")
        });

        ArrayEquals operator = new ArrayEquals(array1, array2);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(false, ((BooleanLiteral)result).getValue());
    }

    @Test
    public void evaluateSimplificationTest() {
        Node left = new Variable("a");
        Node right = new Variable("a");
        ArrayEquals operator = new ArrayEquals(left, right);
        Node result = operator.evaluate();

        assertInstanceOf(BooleanLiteral.class, result);
        assertEquals(true, ((BooleanLiteral)result).getValue());
    }
}
