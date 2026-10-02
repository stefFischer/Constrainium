package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class SpectralCentroidTest {

    @Test
    void evaluatesSpectralCentroid() {
        int sampleRate = 1000;
        int windowSize = 256;
        int length = 1024;
        List<Value<?>> values = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            double t = (double) i / sampleRate;
            double value = Math.sin(2 * Math.PI * 50 * t) + Math.sin(2 * Math.PI * 150 * t);
            values.add(new NumberLiteral(value));
        }
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                values.toArray(new Value<?>[0])
        );

        SpectralCentroid operation =
                new SpectralCentroid(
                        array,
                        new IntegerLiteral(windowSize),
                        new NumberLiteral(sampleRate)
                );

        Node result = operation.evaluate();

        NumberLiteral centroid = assertInstanceOf(NumberLiteral.class, result);
        assertEquals(100.0, centroid.getValue().doubleValue(),5.0);
    }
}
