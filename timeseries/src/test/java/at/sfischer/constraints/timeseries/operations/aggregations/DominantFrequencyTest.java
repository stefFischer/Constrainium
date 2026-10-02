package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class DominantFrequencyTest {

    @Test
    void findsDominantFrequency() {
        int sampleRate = 1000;
        int windowSize = 256;
        int length = 1024;
        List<Value<?>> values = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            double t = (double) i / sampleRate;
            double value = 2.0 * Math.sin(2 * Math.PI * 50 * t) + 0.5 * Math.sin(2 * Math.PI * 150 * t);
            values.add(new NumberLiteral(value));
        }
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                values.toArray(new Value<?>[0])
        );

        DominantFrequency operation =
                new DominantFrequency(
                        array,
                        new IntegerLiteral(windowSize),
                        new NumberLiteral(sampleRate)
                );

        Node result = operation.evaluate();

        NumberLiteral frequency = assertInstanceOf(NumberLiteral.class, result);
        /*
         * Welch resolution is:
         *
         * 1000 / 256 = 3.90625 Hz
         *
         * Therefore the detected frequency will be
         * the nearest available frequency bin to 50 Hz.
         */
        assertEquals(50.78125, frequency.getValue().doubleValue(), 1e-10);
    }
}
