package at.sfischer.constraints.timeseries;

import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.MetamorphicRelationSetTemplate;
import at.sfischer.constraints.NamedExpression;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.parser.ParseException;
import at.sfischer.constraints.timeseries.generator.MeteorsonTransformer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class MainTransformMeteorData {

    public static void main(String[] args) throws IOException, ParseException {
        String dataPath = "C:\\Users\\Stefan Fischer\\IdeaProjects\\time-series_datasets\\f1\\chronos-2-forecast.jsonl";

        Path inputDirectory = Path.of(dataPath);

        ConstraintTemplateFile file = Constants.parse(Constants.RELATION_PARTS);
        List<MetamorphicRelationSetTemplate> relations = file.getConstraints(MetamorphicRelationSetTemplate.class);

        try (Stream<Path> paths = Files.walk(inputDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        for (MetamorphicRelationSetTemplate relation : relations) {
                            for (NamedExpression expression : relation.getTransformations()) {
                                String name = expression.name();
                                Function<List<Double>, List<Double>> transformation = adapt(expression.expression(), "forecastrequest.context");

                                Path outputPath = path.getParent().resolve(path.getFileName().toString().replace(".json", "_" + name + ".json"));
                                if (Files.exists(outputPath)) {
                                    return;
                                }
                                
                                MeteorsonTransformer transformer = new MeteorsonTransformer();
                                try {
                                    transformer.transform(
                                            path,
                                            outputPath,
                                            transformation
                                    );
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    });
        }
    }

    private static Function<List<Double>, List<Double>> adapt(Node myTransformation, String contextVariableName) {
        return context -> {
            ArrayValues<NumberLiteral> input = new ArrayValues<>(
                    TypeEnum.NUMBER,
                    context.stream()
                            .map(NumberLiteral::new)
                            .toArray(NumberLiteral[]::new)
            );

            // Apply transformation
            Node transformation = myTransformation.setVariableNameValue(contextVariableName, input);
            Node evaluated = transformation.evaluate();
            if (!(evaluated instanceof ArrayValues)) {
                throw new RuntimeException("The transformed data is not an array values");
            }

            @SuppressWarnings("unchecked")
            ArrayValues<NumberLiteral> result = (ArrayValues<NumberLiteral>) evaluated;
            Value<?>[] values = result.getValue();

            return Arrays.stream(values)
                    .map(value -> (NumberLiteral) value)
                    .map(NumberLiteral::getValue)
                    .map(Number::doubleValue)
                    .toList();
        };
    }
}
