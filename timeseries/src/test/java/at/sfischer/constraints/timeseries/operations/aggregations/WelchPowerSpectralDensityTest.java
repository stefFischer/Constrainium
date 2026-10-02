package at.sfischer.constraints.timeseries.operations.aggregations;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WelchPowerSpectralDensityTest {

    @Test
    void evaluatesDominantFrequency() {
        int sampleRate = 1000;
        int windowSize = 256;
        int length = 1024;
        List<Value<?>> values = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            double t = (double) i / sampleRate;
            double value = Math.sin(2 * Math.PI * 50 * t) + 0.5 * Math.sin(2 * Math.PI * 150 * t);
            values.add(new NumberLiteral(value));
        }

        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.NUMBER,
                values.toArray(new Value<?>[0])
        );
        WelchPowerSpectralDensity operation =
                new WelchPowerSpectralDensity(
                        array,
                        new IntegerLiteral(windowSize),
                        new NumberLiteral(sampleRate)
                );

        Node result = operation.evaluate();

        assertInstanceOf(ArrayValues.class, result);
        ArrayValues<?> spectrum = (ArrayValues<?>) result;
        Value<?>[] spectrumValues = spectrum.getValue();
        assertEquals(windowSize / 2 + 1, spectrumValues.length);
        int frequencyBin50 = (int) Math.round(50.0 * windowSize / sampleRate);
        int frequencyBin150 = (int) Math.round(150.0 * windowSize / sampleRate);
        double power50 = ((Number) spectrumValues[frequencyBin50].getValue()).doubleValue();
        double power150 = ((Number) spectrumValues[frequencyBin150].getValue()).doubleValue();
        assertTrue(power50 > 0);
        assertTrue(power150 > 0);
        assertTrue(power50 > power150);
    }

    @Test
    void rejectsNonNumericArray() {
        ArrayValues<?> array = new ArrayValues<>(
                TypeEnum.STRING,
                new Value<?>[] {
                        new StringLiteral("a"),
                        new StringLiteral("b"),
                        new StringLiteral("c")
                }
        );
        WelchPowerSpectralDensity operation =
                new WelchPowerSpectralDensity(
                        array,
                        new IntegerLiteral(2),
                        new NumberLiteral(1000)
                );

        ValidationContext context = new ValidationContext();
        operation.validate(context);

        assertFalse(context.isValid());
    }
}
