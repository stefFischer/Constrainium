package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpectralEntropyTest {

    @Test
    void concentratedSpectrumHasLowerEntropyThanDistributedSpectrum() {
        int sampleRate = 1000;
        int windowSize = 256;
        int length = 2048;
        List<Value<?>> concentrated = new ArrayList<>();
        List<Value<?>> distributed = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            double t = (double) i / sampleRate;
            concentrated.add(new NumberLiteral(Math.sin(2 * Math.PI * 50 * t)));

            distributed.add(
                    new NumberLiteral(
                            Math.sin(2 * Math.PI * 50 * t)
                                    + Math.sin(2 * Math.PI * 150 * t)
                                    + Math.sin(2 * Math.PI * 300 * t)
                                    + Math.sin(2 * Math.PI * 400 * t)
                    )
            );
        }

        SpectralEntropy concentratedOperation =
                new SpectralEntropy(
                        new ArrayValues<>(
                                TypeEnum.NUMBER,
                                concentrated.toArray(new Value<?>[0])
                        ),
                        new IntegerLiteral(windowSize),
                        new NumberLiteral(sampleRate)
                );
        SpectralEntropy distributedOperation =
                new SpectralEntropy(
                        new ArrayValues<>(
                                TypeEnum.NUMBER,
                                distributed.toArray(new Value<?>[0])
                        ),
                        new IntegerLiteral(windowSize),
                        new NumberLiteral(sampleRate)
                );

        double concentratedEntropy = assertInstanceOf(NumberLiteral.class, concentratedOperation.evaluate()).getValue().doubleValue();
        double distributedEntropy = assertInstanceOf(NumberLiteral.class, distributedOperation.evaluate()).getValue().doubleValue();
        assertTrue(distributedEntropy > concentratedEntropy);
    }
}
