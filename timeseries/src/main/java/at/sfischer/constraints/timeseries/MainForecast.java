package at.sfischer.constraints.timeseries;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.timeseries.driver.ForecastPlatformMeanDriver;
import at.sfischer.constraints.timeseries.generator.ParquetReader;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class MainForecast {

    public static void main(String[] args) throws IOException {
//        String model = "tirex";
        String model = "chronos-2";

        String dataPath = "C:/Users/Stefan Fischer/IdeaProjects/GiftEvalParquet/bitbrains_fast_storage_5T_long/train.parquet";
        ParquetReader inputGenerator = new ParquetReader(dataPath, model);

        File data = new File(dataPath);
        File jsonl = new File(data.getParentFile(), model + "-forecast.jsonl");

        ForecastPlatformMeanDriver driver = new ForecastPlatformMeanDriver();
        InOutputDataCollection sourceData = new InOutputDataCollection();
        AtomicLong counter = new AtomicLong();
        long start = System.nanoTime();
        inputGenerator.generate(null)
                .limit(10)
                .forEach(input -> {
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
                        DataObject output = driver.execute(input);
                        if (output == null) {
                            System.err.println("Model output was null.");
                            output = new DataObject();
                        }

                        sourceData.addDataEntry(input, output);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        sourceData.toJsonl(jsonl);
    }
}
