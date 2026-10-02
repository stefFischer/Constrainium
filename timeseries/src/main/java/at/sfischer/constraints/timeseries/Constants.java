package at.sfischer.constraints.timeseries;

import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.parser.ConstraintDslParser;
import at.sfischer.constraints.parser.ConstraintDslScanner;
import at.sfischer.constraints.parser.ParseException;

import java.io.IOException;
import java.io.StringReader;

public interface Constants {

    String RELATION_PARTS = """
            function addLinearTrend(x, slope){
                arrays.combine(
                    x,
                    arrays.generate(ARRAY_INDEX * slope, arrays.length(x)),
                    ARRAY_LEFT + ARRAY_RIGHT
                )
            }
            
            MRS M1:
                transformations: {
                    # Add Offset
                    @AddOffset1: arrays.forEach(forecastrequest.context, ARRAY_ELEMENT + (0.5 * arrays.standardDeviation(forecastrequest.context)))
                    @AddOffset2: arrays.forEach(forecastrequest.context, ARRAY_ELEMENT + (1.0 * arrays.standardDeviation(forecastrequest.context)))
                    @AddOffset3: arrays.forEach(forecastrequest.context, ARRAY_ELEMENT + (2.0 * arrays.standardDeviation(forecastrequest.context)))
                    @AddAverage: arrays.forEach(forecastrequest.context, ARRAY_ELEMENT + arrays.average(forecastrequest.context))
                    # Scale
                    @Scale1: arrays.forEach(forecastrequest.context, ARRAY_ELEMENT * 1.1)
                    @Scale2: arrays.forEach(forecastrequest.context, ARRAY_ELEMENT * 1.5)
                    @Scale3: arrays.forEach(forecastrequest.context, ARRAY_ELEMENT * 2.0)
                    @Scale4: arrays.forEach(forecastrequest.context, ARRAY_ELEMENT * 0.9)
                    @Scale5: arrays.forEach(forecastrequest.context, ARRAY_ELEMENT * 0.5)
                    @Scale6: arrays.forEach(forecastrequest.context, ARRAY_ELEMENT * 0.25)
                    # Add Noise
                    @AddNoise1: timeseries.addGaussianNoise(forecastrequest.context, 0.05 * arrays.standardDeviation(forecastrequest.context))
                    @AddNoise2: timeseries.addGaussianNoise(forecastrequest.context, 0.1 * arrays.standardDeviation(forecastrequest.context))
                    @AddNoise3: timeseries.addGaussianNoise(forecastrequest.context, 0.25 * arrays.standardDeviation(forecastrequest.context))
                    @AddNoise4: timeseries.addGaussianNoise(forecastrequest.context, 0.5 * arrays.standardDeviation(forecastrequest.context))
                    # Smoothing
                    @Smoothing1: timeseries.movingAverageSmoothing(forecastrequest.context, round(0.025 * arrays.length(forecastrequest.context)))
                    @Smoothing2: timeseries.movingAverageSmoothing(forecastrequest.context, round(0.05 * arrays.length(forecastrequest.context)))
                    @Smoothing3: timeseries.movingAverageSmoothing(forecastrequest.context, round(0.10 * arrays.length(forecastrequest.context)))
                    # Add Trend
                    @AddLinearTrend1: addLinearTrend(forecastrequest.context, 0.01)
                    @AddLinearTrend2: addLinearTrend(forecastrequest.context, 0.05)
                    # Add a 0.5-amplitude, 5 Hz sine wave sampled at 100 Hz
                    @AddSineWave: arrays.combine(
                        forecastrequest.context,
                        arrays.generate(0.5 * sin(2 * PI * 5 * ARRAY_INDEX / 100), arrays.length(forecastrequest.context)),
                        ARRAY_LEFT + ARRAY_RIGHT
                    )
                    # Add Outliers
                    @AddOutliers1: timeseries.addOutliers(forecastrequest.context, arrays.length(forecastrequest.context) / 25, 1)
                    @AddOutliers2: timeseries.addOutliers(forecastrequest.context, arrays.length(forecastrequest.context) / 25, 2)
                    @AddOutliers3: timeseries.addOutliers(forecastrequest.context, arrays.length(forecastrequest.context) / 25, 4)
                    # Outlier clipping
                    @ClipOutliers: arrays.forEach(forecastrequest.context, min(max(ARRAY_ELEMENT, arrays.percentile(forecastrequest.context, 5)), arrays.percentile(forecastrequest.context, 95)))
                    # Time reversal
                    @Reverse: arrays.reverse(forecastrequest.context)
                }

                # PROPERTIES
                validations: {
                    # Level
                    @Mean: arrays.average(x)
                    @Median: arrays.median(x)
                    # Scale
                    @Amplitude: arrays.max(x) - arrays.min(x)
                    @Amplitude90p: arrays.percentile(x, 95) - arrays.percentile(x, 5)
                    @Std: arrays.standardDeviation(x)
                    # Local movement
                    @AbsoluteDerivative: timeseries.averageAbsoluteDerivative(x)
                    # Trend
                    @LinearTrend: timeseries.linearTrend(x)
                    # Auto Correlation
                    @AutoCorrelation: timeseries.autoCorrelation(x, 1)
                    # Distribution shape
                    @Skewness: timeseries.skewness(x)
                    @Kurtosis: timeseries.pearsonKurtosis(x)
                    # Frequency location
                    @DominantFrequency: timeseries.dominantFrequency(x, 256, 1000)
                    @SpectralCentroid: timeseries.spectralCentroid(x, 256, 1000)
                    # Frequency distribution
                    @SpectralEntropy: timeseries.spectralEntropy(x, 256, 1000)
                    # Oscillation
                    @ZeroCrossingRate: timeseries.levelCrossingRate(x, 0)
                    @MeanCrossingRate: timeseries.levelCrossingRate(x, arrays.average(x))
                    # Directional complexity
                    @TurningPointRate: timeseries.turningPointRate(x)
                }
            """;

    static ConstraintTemplateFile parse(String source) throws IOException, ParseException {
        ConstraintDslScanner scanner = new ConstraintDslScanner(new StringReader(source));
        ConstraintDslParser parser = new ConstraintDslParser(scanner);
        return parser.parse();
    }
}
