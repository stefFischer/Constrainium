package at.sfischer.constraints.timeseries.generator;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.model.ArrayValues;
import at.sfischer.constraints.model.IntegerLiteral;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.timeseries.driver.ForecastPlatformMeanDriver;
import at.sfischer.generator.InputGenerator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PowerSystemCsvReader implements InputGenerator {

    private final String model;
    private final Path csvPath;
    private final Pattern columnPattern;
    private final int contextSize;
    private final int groundTruthSize;
    private final int stride;

    private List<String> matchingColumns;
    private List<double[]> series;

    private int currentSeries = 0;
    private int currentWindowStart = 0;

    public PowerSystemCsvReader(
            String csvPath,
            String columnRegex,
            String model,
            int contextSize,
            int groundTruthSize,
            int stride
    ) {
        if (contextSize <= 0) {
            throw new IllegalArgumentException("contextSize must be > 0");
        }

        if (groundTruthSize <= 0) {
            throw new IllegalArgumentException("groundTruthSize must be > 0");
        }

        this.csvPath = Path.of(csvPath);
        this.columnPattern = Pattern.compile(columnRegex);
        this.model = model;
        this.contextSize = contextSize;
        this.groundTruthSize = groundTruthSize;
        this.stride = stride;

        loadData();
    }

    @Override
    public String getIdentifier() {
        return "PowerSystemCsvReader";
    }

    @Override
    public Stream<DataObject> generate(SimpleDataSchema schema) {
        return Stream.generate(() -> generateInput(schema))
                .takeWhile(Objects::nonNull);
    }

    private DataObject generateInput(SimpleDataSchema schema) {
        if (matchingColumns == null || matchingColumns.isEmpty()) {
            return null;
        }

        while (currentSeries < series.size()) {
            double[] values = series.get(currentSeries);
            int windowSize = contextSize + groundTruthSize;
            if (currentWindowStart + windowSize > values.length) {
                currentSeries++;
                currentWindowStart = 0;
                continue;
            }

            int start = currentWindowStart;
            currentWindowStart += stride;
            if (!isValidWindow(values, start, windowSize)) {
                continue;
            }

            ArrayValues<NumberLiteral> context = createArray(values, start, contextSize);
            ArrayValues<NumberLiteral> groundTruth = createArray(values, start + contextSize, groundTruthSize);
            IntegerLiteral horizon = new IntegerLiteral(groundTruthSize);
            return ForecastPlatformMeanDriver.createInput(
                    model,
                    context,
                    horizon,
                    groundTruth
            );
        }

        return null;
    }

    private boolean isValidWindow(
            double[] values,
            int start,
            int length
    ) {
        for (int i = start; i < start + length; i++) {
            if (!Double.isFinite(values[i])) {
                return false;
            }
        }

        return true;
    }

    private static ArrayValues<NumberLiteral> createArray(
            double[] values,
            int start,
            int length
    ) {
        NumberLiteral[] result = new NumberLiteral[length];
        for (int i = 0; i < length; i++) {
            result[i] = new NumberLiteral(values[start + i]);
        }

        return new ArrayValues<>(TypeEnum.NUMBER, result);
    }

    private void loadData() {
        try (Reader reader = Files.newBufferedReader(csvPath);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            List<String> headers = parser.getHeaderNames();
            matchingColumns = headers.stream()
                    .filter(columnPattern.asPredicate())
                    .toList();

            if (matchingColumns.isEmpty()) {
                throw new IllegalArgumentException("No CSV columns matched regex: " + columnPattern.pattern());
            }

            List<List<Double>> valuesByColumn = new ArrayList<>();
            for (int i = 0; i < matchingColumns.size(); i++) {
                valuesByColumn.add(new ArrayList<>());
            }

            for (CSVRecord record : parser) {
                for (int i = 0; i < matchingColumns.size(); i++) {
                    String column = matchingColumns.get(i);
                    String value = record.get(column);

                    if (value == null || value.isBlank()) {
                        valuesByColumn.get(i).add(Double.NaN);
                    } else {
                        valuesByColumn.get(i).add(parseValue(value));
                    }
                }
            }

            series = new ArrayList<>();
            for (List<Double> values : valuesByColumn) {
                double[] array = new double[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    array[i] = values.get(i);
                }
                series.add(array);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read power system CSV: " + csvPath, e);
        }
    }

    private static double parseValue(String value) {
        if (value == null || value.isBlank()) {
            return Double.NaN;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
