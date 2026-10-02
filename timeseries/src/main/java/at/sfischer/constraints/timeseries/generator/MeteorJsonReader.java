package at.sfischer.constraints.timeseries.generator;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.model.ArrayValues;
import at.sfischer.constraints.model.IntegerLiteral;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.timeseries.driver.ForecastPlatformMeanDriver;
import at.sfischer.generator.InputGenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class MeteorJsonReader implements InputGenerator {

    private final ObjectMapper objectMapper;
    private final String model;
    private final Path jsonPath;

    private Iterator<JsonNode> windows;

    public MeteorJsonReader(
            String pathToJsonFile,
            String model
    ) {
        this.model = model;
        this.jsonPath = Path.of(pathToJsonFile);
        this.objectMapper = new ObjectMapper();

        loadWindows();
    }

    @Override
    public String getIdentifier() {
        return "MeteorJsonReader";
    }

    @Override
    public Stream<DataObject> generate(SimpleDataSchema schema) {
        return Stream.generate(() -> generateInput(schema))
                .takeWhile(Objects::nonNull);
    }

    private DataObject generateInput(SimpleDataSchema schema) {
        if (!windows.hasNext()) {
            return null;
        }

        JsonNode window = windows.next();

        ArrayValues<NumberLiteral> context =
                doubleArrayValue(window, "context_data");

        ArrayValues<NumberLiteral> groundTruth =
                doubleArrayValue(window, "ground_truth");

        IntegerLiteral horizon =
                new IntegerLiteral(groundTruth.getValue().length);

        return ForecastPlatformMeanDriver.createInput(
                model,
                context,
                horizon,
                groundTruth
        );
    }

    private void loadWindows() {
        try {
            JsonNode root = objectMapper.readTree(jsonPath.toFile());
            JsonNode windowsNode = root.get("windows");
            if (windowsNode == null || !windowsNode.isObject()) {
                throw new IllegalArgumentException(
                        "JSON does not contain a 'windows' object: "
                                + jsonPath
                );
            }

            List<JsonNode> windowList = new ArrayList<>();
            Iterator<JsonNode> iterator = windowsNode.elements();
            while (iterator.hasNext()) {
                windowList.add(iterator.next());
            }

            windows = windowList.iterator();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read JSON file: " + jsonPath,
                    e
            );
        }
    }

    private static ArrayValues<NumberLiteral> doubleArrayValue(
            JsonNode window,
            String field
    ) {
        JsonNode values = window.get(field);
        if (values == null || !values.isArray()) {
            throw new IllegalArgumentException(
                    "Window does not contain array field '" + field + "'"
            );
        }

        List<NumberLiteral> result = new ArrayList<>();
        for (JsonNode value : values) {
            if (value.isNull()) {
                continue;
            }

            result.add(new NumberLiteral(value.asDouble()));
        }

        NumberLiteral[] array = result.toArray(new NumberLiteral[0]);
        return new ArrayValues<>(TypeEnum.NUMBER, array);
    }
}