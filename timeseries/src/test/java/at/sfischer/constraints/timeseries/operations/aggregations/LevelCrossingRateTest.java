package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.ArrayValues;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class LevelCrossingRateTest {

    @Test
    void evaluatesZeroCrossingRate() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(-1),
                        new NumberLiteral(1),
                        new NumberLiteral(-1),
                        new NumberLiteral(1)
                }
        );
        LevelCrossingRate operation =
                new LevelCrossingRate(
                        array,
                        new NumberLiteral(0)
                );

        NumberLiteral result = assertInstanceOf(
                NumberLiteral.class,
                operation.evaluate()
        );

        assertEquals(1.0, result.getValue().doubleValue());
    }

    @Test
    void countsCrossingsAtSpecifiedLevel() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(1),
                        new NumberLiteral(5),
                        new NumberLiteral(3),
                        new NumberLiteral(1),
                        new NumberLiteral(-2),
                        new NumberLiteral(-5),
                        new NumberLiteral(2)
                }
        );
        LevelCrossingRate operation =
                new LevelCrossingRate(
                        array,
                        new NumberLiteral(0)
                );

        NumberLiteral result =
                assertInstanceOf(
                        NumberLiteral.class,
                        operation.evaluate()
                );

        assertEquals(1.0 / 3.0, result.getValue().doubleValue(), 1e-10);
    }

    @Test
    void usesSpecifiedLevel() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                new Value<?>[] {
                        new NumberLiteral(4),
                        new NumberLiteral(6),
                        new NumberLiteral(4),
                        new NumberLiteral(6)
                }
        );
        LevelCrossingRate operation =
                new LevelCrossingRate(
                        array,
                        new NumberLiteral(5)
                );

        NumberLiteral result =
                assertInstanceOf(
                        NumberLiteral.class,
                        operation.evaluate()
                );

        assertEquals(1.0, result.getValue().doubleValue());
    }
}
