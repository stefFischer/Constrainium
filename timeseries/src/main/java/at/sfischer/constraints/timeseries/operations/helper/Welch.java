package at.sfischer.constraints.timeseries.operations.helper;

public final class Welch {

    private Welch() {}

    public static WelchSpectrum calculate(
            double[] signal,
            int windowSize,
            double samplingRate
    ) {
        if (windowSize < 2) {
            throw new IllegalArgumentException("Window size must be at least 2.");
        }

        if (samplingRate <= 0) {
            throw new IllegalArgumentException("Sampling rate must be positive.");
        }

        if (signal.length < windowSize) {
            throw new IllegalArgumentException("Signal is shorter than the window size.");
        }

        int step = windowSize / 2;
        int frequencyBins = windowSize / 2 + 1;
        double[] powers = new double[frequencyBins];
        double[] frequencies = new double[frequencyBins];
        for (int k = 0; k < frequencyBins; k++) {
            frequencies[k] = k * samplingRate / windowSize;
        }

        double[] window = new double[windowSize];
        double windowPower = 0;
        for (int i = 0; i < windowSize; i++) {
            window[i] = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / (windowSize - 1)));
            windowPower += window[i] * window[i];
        }

        int segmentCount = 0;
        for (int start = 0; start + windowSize <= signal.length; start += step) {
            double mean = 0;
            for (int i = 0; i < windowSize; i++) {
                mean += signal[start + i];
            }
            mean /= windowSize;
            double[] segment = new double[windowSize];
            for (int i = 0; i < windowSize; i++) {
                segment[i] = (signal[start + i] - mean) * window[i];
            }

            for (int k = 0; k < frequencyBins; k++) {
                double real = 0;
                double imaginary = 0;
                for (int n = 0; n < windowSize; n++) {
                    double angle = 2.0 * Math.PI * k * n / windowSize;
                    real += segment[n] * Math.cos(angle);
                    imaginary -= segment[n] * Math.sin(angle);
                }

                double magnitudeSquared = real * real + imaginary * imaginary;
                double power = magnitudeSquared / (samplingRate * windowPower);
                if (k > 0 && !(windowSize % 2 == 0 && k == windowSize / 2)) {
                    power *= 2.0;
                }

                powers[k] += power;
            }

            segmentCount++;
        }

        if (segmentCount == 0) {
            throw new IllegalArgumentException("No Welch segments could be calculated.");
        }

        for (int k = 0; k < powers.length; k++) {
            powers[k] /= segmentCount;
        }

        return new WelchSpectrum(
                frequencies,
                powers
        );
    }
}
