package at.sfischer.constraints.timeseries;

import at.sfischer.constraints.ConstraintTemplateFile;
import at.sfischer.constraints.MetamorphicRelationSetTemplate;
import at.sfischer.constraints.NamedExpression;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.model.DataReference;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Value;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.parser.ParseException;
import at.sfischer.constraints.timeseries.driver.BatchRequest;
import at.sfischer.constraints.timeseries.driver.ForecastPlatformMeanBatchDriver;
import at.sfischer.constraints.timeseries.driver.ForecastPlatformMeanDriver;
import at.sfischer.driver.SystemDriver;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class MainTransformForecast {

    public static void main(String[] args) throws IOException, ParseException {
//        String dataPath = "C:/Users/sfischer/Desktop/METeOR-SPM/data/PowerSystemData/chronos-2-forecast.jsonl";
//        String dataPath = "C:/Users/Stefan Fischer/IdeaProjects/time-series_datasets/energy_load_values_EU_2025/chronos-2-forecast.jsonl";
        String dataPath = "C:/Users/Stefan Fischer/IdeaProjects/time-series_datasets/m4monthly/tirex-2-forecast.jsonl";
//        String dataPath = "C:/Users/Stefan Fischer/IdeaProjects/time-series_datasets/m4monthly/chronos-2-forecast.jsonl";

        boolean batched = true;
        int batchSize = 100;

        ConstraintTemplateFile file = Constants.parse(Constants.RELATION_PARTS);
        List<MetamorphicRelationSetTemplate> relations = file.getConstraints(MetamorphicRelationSetTemplate.class);

        SimpleDataCollection sourceInputData = new SimpleDataCollection();
        File jsonlInput =  new File(dataPath);
        InOutputDataCollection forecastData = InOutputDataCollection.parseData(jsonlInput);
        forecastData.getDataCollection().stream()
//                .limit(10)
                .forEach(dataPair -> sourceInputData.addDataEntry(
                        (DataObject) dataPair.getValue0().getDataValue("input").getValue()
                ));

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
                File jsonl = new File(data.getParentFile(), jsonlInput.getName().replace(".jsonl", "") + "-" + transformationIdentifier + ".jsonl");
                if(jsonl.exists()) {
                    continue;
                }

                SystemDriver driver = batched
                        ? new ForecastPlatformMeanBatchDriver()
                        : new ForecastPlatformMeanDriver();

                InOutputDataCollection sourceData = new InOutputDataCollection();
                AtomicLong counter = new AtomicLong();
                long start = System.nanoTime();

                if (batched) {
                    List<DataObject> batch = new ArrayList<>(batchSize);

                    sourceInputData.getDataCollection().forEach(input -> {
                        DataObject transformedInput = transform(transformation, input, "forecastrequest.context");
                        if (transformedInput == null) {
                            System.out.println("Transformation failed");
                            System.out.println(transformationIdentifier);

                            try {
                                System.out.println(input.toJson());
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }

                            // Preserve the original behavior of recording failed
                            // transformations as empty input/output objects.
                            transformedInput = new DataObject();
                        }

                        batch.add(transformedInput);

                        if (batch.size() >= batchSize) {
                            processBatch(batch, driver, sourceData, counter, start);
                            batch.clear();
                        }
                    });

                    // Process the final, potentially incomplete batch.
                    if (!batch.isEmpty()) {
                        processBatch(batch, driver, sourceData, counter, start);
                    }
                } else {
                    sourceInputData.getDataCollection().forEach(input -> {
                        DataObject transformedInput = transform(transformation, input, "forecastrequest.context");
                        if (transformedInput == null) {
                            System.out.println("Transformation failed");
                            System.out.println(transformationIdentifier);

                            try {
                                System.out.println(input.toJson());
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }

                            transformedInput = new DataObject();
                        }

                        try {
                            DataObject output;

                            if(transformedInput == null){
                                transformedInput = new DataObject();
                                output = new DataObject();
                            } else {
                                output = driver.execute(transformedInput);

                                if (output == null) {
                                    System.err.println("Model output was null.");
                                    output = new DataObject();
                                }
                            }

                            sourceData.addDataEntry(transformedInput, output);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        long n = counter.incrementAndGet();
                        if (n % 100 == 0) {
                            reportThroughput(n, start);
                        }
                    });
                }

//                ForecastPlatformMeanDriver driver = new ForecastPlatformMeanDriver();
//                InOutputDataCollection sourceData = new InOutputDataCollection();
//                AtomicLong counter = new AtomicLong();
//                long start = System.nanoTime();
//
//                sourceInputData.getDataCollection().forEach(input -> {
//                    DataObject transformedInput = transform(transformation, input, "forecastrequest.context");
//
//                    if(transformedInput == null){
//                        System.out.println("Transformation failed");
//                        System.out.println(transformationIdentifier);
//                        try {
//                            System.out.println(input.toJson());
//                        } catch (JsonProcessingException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//
//                    long n = counter.incrementAndGet();
//                    if (n % 100 == 0) {
//                        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
//                        System.out.printf(
//                                "Processed %,d inputs (%,.1f inputs/s)%n",
//                                n,
//                                n / seconds
//                        );
//                    }
//
//                    try {
//                        DataObject output;
//                        if(transformedInput != null){
//                            output = driver.execute(transformedInput);
//                            if (output == null) {
//                                System.err.println("Model output was null.");
//                                output = new DataObject();
//                            }
//                        } else {
//                            transformedInput = new DataObject();
//                            output = new DataObject();
//                        }
//
//                        sourceData.addDataEntry(transformedInput, output);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                });

                sourceData.toJsonl(jsonl);

                transformationIndex++;
            }
        }
    }

    public static DataObject transform(Node transformation, DataObject value, String toReplace){
        Set<Variable> constraintVariables = transformation.findInvolvedVariables();
        List<Map<Variable, Node>> valueCombinations = Utils.collectValueCombinations(value, constraintVariables);
        for (Map<Variable, Node> valueCombination : valueCombinations) {
            Node transformed = transformation.setVariableValues(valueCombination);
            transformed = transformed.evaluate();
            if(transformed instanceof Value<?> val) {
                DataObject transformedValue = value.clone();
                transformedValue.putNodeValue(toReplace, val);
                return transformedValue;
            }
        }

        return null;
    }


    private static void processBatch(
            List<DataObject> batchInputs,
            SystemDriver driver,
            InOutputDataCollection sourceData,
            AtomicLong counter,
            long start
    ) {
        for (BatchRequest batch : ForecastPlatformMeanBatchDriver.createInputs(batchInputs)) {
            try {
                DataObject output = driver.execute(batch.input());

                if (output == null) {
                    System.err.println("Model output was null.");
                    continue;
                }

                DataValue<?> forecastValue = output.getDataValue("forecast");
                Object forecastObject = forecastValue.getValue();

                // The batch driver should return one forecast per original input.
                // Adapt this extraction to the actual array type returned by your API.
                DataValue<?>[] forecasts = (DataValue<?>[]) forecastObject;
                if (forecasts.length != batch.originalInputs().size()) {
                    throw new IllegalStateException(
                            "Batch output count does not match input count: "
                                    + forecasts.length + " forecasts for "
                                    + batch.originalInputs().size() + " inputs"
                    );
                }

                for (int i = 0; i < forecasts.length; i++) {
                    DataObject individualOutput = new DataObject();
                    individualOutput.putValue("model",
                            batch.input().getDataValue("batchforecastrequest")
                                    .getValue() instanceof DataObject body
                                    ? (String) body.getDataValue("model").getValue()
                                    : null);
                    individualOutput.putValue("forecast", (Number[]) forecasts[i].getValue());

                    sourceData.addDataEntry(
                            batch.originalInputs().get(i),
                            individualOutput
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Count original inputs, not grouped requests.
        long n = counter.addAndGet(batchInputs.size());
        reportThroughput(n, start);
    }

    private static void reportThroughput(long n, long start) {
        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
        // Report at roughly every 100 original inputs.
        System.out.printf(
                "Processed %,d inputs (%,.1f inputs/s)%n",
                n,
                n / seconds
        );
    }
}
