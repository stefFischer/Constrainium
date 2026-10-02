package at.sfischer.constraints.timeseries.operations.helper;

public class WelchSpectrum {

    private final double[] frequencies;
    private final double[] powers;

    public WelchSpectrum(
            double[] frequencies,
            double[] powers
    ) {
        if (frequencies.length != powers.length) {
            throw new IllegalArgumentException("Frequency and power arrays must have the same length.");
        }

        this.frequencies = frequencies;
        this.powers = powers;
    }

    public double[] getFrequencies() {
        return frequencies;
    }

    public double[] getPowers() {
        return powers;
    }

    public int size(){
        return this.frequencies.length;
    }
}