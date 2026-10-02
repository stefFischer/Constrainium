package at.sfischer.constraints.timeseries;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataValue;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.timeseries.driver.BatchRequest;
import at.sfischer.constraints.timeseries.driver.ForecastPlatformMeanBatchDriver;
import at.sfischer.constraints.timeseries.driver.ForecastPlatformMeanDriver;
import at.sfischer.constraints.timeseries.generator.MeteorJsonDirectoryReader;
import at.sfischer.driver.SystemDriver;
import at.sfischer.generator.InOutputCollectionInputGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MainForecast {

    public static void main(String[] args) throws IOException {
//        String model = "tirex";
//        String model = "chronos-2";
//        String model = "chronos-2-small";
        String model = "tirex-2";
//        String model = "timesfm-2.5-200m";
//        String model = "toto-2.0-4m";
//        String model = "toto-2.0-1B";
//        String model = "timemoe-200m";
//        String model = "timemoe-50m";

        boolean batched = true;
        int batchSize = 100;

//        String dataPath = "C:/Users/Stefan Fischer/IdeaProjects/GiftEvalParquet/bitbrains_fast_storage_5T_long/train.parquet";
//        GiftEvalParquetReader inputGenerator = new GiftEvalParquetReader(dataPath, model);

//        String dataPath = "C:/Users/sfischer/Desktop/METeOR-SPM/data/PowerSystemData/time_series_15min_singleindex.csv";
//        PowerSystemCsvReader inputGenerator = new PowerSystemCsvReader(
//                dataPath,
//                "[A-Z]{2}_load_actual_entsoe_transparency",
//                model,
//                1344,
//                288,
//                1344);

        String dataPath = "C:\\Users\\Stefan Fischer\\IdeaProjects\\time-series_datasets\\m4monthly\\prepared_windows";
        MeteorJsonDirectoryReader inputGenerator = new MeteorJsonDirectoryReader(dataPath, model);

//        String dataPath = "C:/Users/Stefan Fischer/IdeaProjects/time-series_datasets/energy_load_values_EU_2025/reference-chronos-2-forecast-single-for-inputs.jsonl";
//        InOutputDataCollection forecastData = InOutputDataCollection.parseData(new File(dataPath));
//        forecastData.getDataCollection().forEach(pair -> pair.getValue0().putValue("input.forecastrequest.model", model));
//        InOutputCollectionInputGenerator inputGenerator = new InOutputCollectionInputGenerator(forecastData);

        File data = new File(dataPath);
        File jsonl = new File(data.getParentFile(), model + "-forecast.jsonl");

        SystemDriver driver = batched ?
                new ForecastPlatformMeanBatchDriver() :
                new ForecastPlatformMeanDriver();
        InOutputDataCollection sourceData = new InOutputDataCollection();
        AtomicLong counter = new AtomicLong();
        long start = System.nanoTime();


        if (batched) {
            List<DataObject> batch = new ArrayList<>(batchSize);
            inputGenerator.generate(null)
//                    .limit(200)
                    .forEach(input -> {
                batch.add(input);

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
            inputGenerator.generate(null)
//                    .limit(200)
                    .forEach(input -> {
                try {
                    DataObject output = driver.execute(input);
                    if (output == null) {
                        System.err.println("Model output was null.");
                        output = new DataObject();
                    }

                    sourceData.addDataEntry(input, output);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                long n = counter.incrementAndGet();
                if (n % 100 == 0) {
                    reportThroughput(n, start);
                }
            });
        }

        sourceData.toJsonl(jsonl);
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
