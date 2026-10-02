package at.sfischer.constraints.timeseries.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MeteorsonTransformer {

    private final ObjectMapper objectMapper;

    public MeteorsonTransformer() {
        this.objectMapper = new ObjectMapper();
    }

    public void transform(
            Path input,
            Path output,
            Function<List<Double>, List<Double>> transformation
    ) throws IOException {
        JsonNode root = objectMapper.readTree(input.toFile());
        JsonNode windows = root.get("windows");
        if (windows == null || !windows.isObject()) {
            throw new IllegalArgumentException(
                    "Input does not contain a 'windows' object"
            );
        }

        Iterator<Map.Entry<String, JsonNode>> iterator = windows.fields();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            JsonNode window = entry.getValue();

            List<Double> context = readDoubleArray(window.get("context_data"));
            List<Double> transformed = transformation.apply(context);

            replaceDoubleArray(
                    (ObjectNode) window,
                    "context_data",
                    transformed
            );

            updateNormalization((ObjectNode) window);
        }

        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(output.toFile(), root);
    }

    private static List<Double> readDoubleArray(JsonNode node) {
        List<Double> result = new ArrayList<>();
        for (JsonNode value : node) {
            result.add(value.asDouble());
        }

        return result;
    }

    private static void replaceDoubleArray(
            ObjectNode object,
            String field,
            List<Double> values
    ) {
        ArrayNode array = object.putArray(field);
        for (Double value : values) {
            array.add(value);
        }
    }

    private static void updateNormalization(
            ObjectNode window
    ) {
        List<Double> context =
                readDoubleArray(window.get("context_data"));

        double mean = context.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElseThrow();

        double variance = context.stream()
                .mapToDouble(x -> Math.pow(x - mean, 2))
                .average()
                .orElse(0.0);

        double originalStd = Math.sqrt(variance);

        double eps = 1e-8;
        boolean stdWasAdjusted = originalStd < eps;

        double std = stdWasAdjusted
                ? eps
                : originalStd;

        replaceDoubleArray(
                window,
                "normalized_context_data",
                context.stream()
                        .map(x -> (x - mean) / std)
                        .toList()
        );

        ObjectNode normalization = (ObjectNode) window.get("normalization");

        normalization.put("mean", mean);
        normalization.put("std", std);
        normalization.put("original_std", originalStd);
        normalization.put("std_was_adjusted", stdWasAdjusted);
        normalization.put("eps", eps);
    }
}