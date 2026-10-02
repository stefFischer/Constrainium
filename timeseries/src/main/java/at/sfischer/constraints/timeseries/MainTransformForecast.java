package at.sfischer.constraints.timeseries;

import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.MetamorphicRelationSetTemplate;
import at.sfischer.constraints.NamedExpression;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.parser.ConstraintDslParser;
import at.sfischer.constraints.parser.ConstraintDslScanner;
import at.sfischer.constraints.parser.ParseException;
import at.sfischer.constraints.timeseries.driver.ForecastPlatformMeanDriver;
import at.sfischer.constraints.timeseries.generator.ParquetReader;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class MainTransformForecast {

    public static void main(String[] args) throws IOException, ParseException {
//        String model = "tirex";
        String model = "chronos-2";

        String dataPath = "C:/Users/Stefan Fischer/IdeaProjects/GiftEvalParquet/bitbrains_fast_storage_5T_long/train.parquet";
        ParquetReader inputGenerator = new ParquetReader(dataPath, model);

        ConstraintTemplateFile file = Constants.parse(Constants.RELATION_PARTS);
        List<MetamorphicRelationSetTemplate> relations = file.getConstraints(MetamorphicRelationSetTemplate.class);

        SimpleDataCollection sourceInputData = new SimpleDataCollection();
        inputGenerator.generate(null)
                .limit(10)
                .forEach(sourceInputData::addDataEntry);

        SimpleDataSchema inputSchema = sourceInputData.deriveSchema(null);
        DataSchemaEntry<SimpleDataSchema> context = inputSchema.findDataSchemaEntry("forecastrequest.context");
        DataSchemaEntry<SimpleDataSchema> horizon = inputSchema.findDataSchemaEntry("forecastrequest.horizon");

        int transformationIndex = 0;
        for (MetamorphicRelationSetTemplate relation : relations) {
            for (NamedExpression expression : relation.getTransformations()) {
                String name = expression.name();
                // Replace variable in transformation with the input schema references.
                Node transformation = expression.expression().setVariableNameValues(Map.of(
                        "forecastrequest.context", new DataReference(context),
                        "forecastrequest.horizon", new DataReference(horizon)
                ));

                File data = new File(dataPath);
                String transformationIdentifier = name != null ? name : transformationIndex + "";
                File jsonl = new File(data.getParentFile(), model + "-" + transformationIdentifier + "-forecast.jsonl");

                ForecastPlatformMeanDriver driver = new ForecastPlatformMeanDriver();
                InOutputDataCollection sourceData = new InOutputDataCollection();
                AtomicLong counter = new AtomicLong();
                long start = System.nanoTime();

                sourceInputData.getDataCollection().forEach(input -> {
                    DataObject transformedInput = transform(transformation, input);

                    long n = counter.incrementAndGet();
                    if (n % 100 == 0) {
                        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
                        System.out.printf(
                                "Processed %,d inputs (%,.1f inputs/s)%n",
                                n,
                                n / seconds
                        );
                    }

                    try {
                        DataObject output;
                        if(transformedInput != null){
                            output = driver.execute(transformedInput);
                            if (output == null) {
                                System.err.println("Model output was null.");
                                output = new DataObject();
                            }
                        } else {
                            transformedInput = new DataObject();
                            output = new DataObject();
                        }

                        sourceData.addDataEntry(transformedInput, output);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                sourceData.toJsonl(jsonl);

                transformationIndex++;
            }
        }
    }

    public static DataObject transform(Node transformation, DataObject value){
        Set<Variable> constraintVariables = transformation.findInvolvedVariables();
        List<Map<Variable, Node>> valueCombinations = Utils.collectValueCombinations(value, constraintVariables);
        for (Map<Variable, Node> valueCombination : valueCombinations) {
            Node transformed = transformation.setVariableValues(valueCombination);
            transformed = transformed.evaluate();
            if(transformed instanceof Value<?> val) {
                DataObject transformedValue = value.clone();
                transformedValue.putNodeValue(valueCombination.keySet().iterator().next().getName(), val);
                return transformedValue;
            }
        }

        return null;
    }
}
