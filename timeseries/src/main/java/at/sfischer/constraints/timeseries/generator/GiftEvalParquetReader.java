package at.sfischer.constraints.timeseries.generator;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataSchema;
import at.sfischer.constraints.model.ArrayValues;
import at.sfischer.constraints.model.IntegerLiteral;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.timeseries.driver.ForecastPlatformMeanDriver;
import at.sfischer.generator.InputGenerator;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class GiftEvalParquetReader implements InputGenerator {

    private final org.apache.parquet.hadoop.ParquetReader<GenericRecord> reader;

    private final String model;

    public GiftEvalParquetReader(String pathToParquetFile, String model) {
        this.model = model;
        Path path = new Path(pathToParquetFile);
        try {
            this.reader = AvroParquetReader.<GenericRecord>builder(path).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getIdentifier() {
        return "GiftEvalParquetReader";
    }

    @Override
    public Stream<DataObject> generate(SimpleDataSchema schema) {
        return Stream.generate(() -> generateInput(schema))
                .takeWhile(Objects::nonNull);
    }

    private DataObject generateInput(SimpleDataSchema schema) {
        try {
            GenericRecord record = reader.read();
            if(record == null){
                return null;
            }

            ArrayValues<NumberLiteral> context = doubleArrayValue(record, "history_value");
            ArrayValues<NumberLiteral> groundTruth = doubleArrayValue(record, "future_value");
            int h = groundTruth.getValue().length;
            IntegerLiteral horizon = new IntegerLiteral(h);
            return ForecastPlatformMeanDriver.createInput(this.model, context, horizon, groundTruth);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ArrayValues<NumberLiteral> doubleArrayValue(
            GenericRecord record,
            String field
    ) {
        List<?> values = (List<?>) record.get(field);
        List<NumberLiteral> result = new ArrayList<>();
        for (Object value : values) {
            GenericRecord element = (GenericRecord) value;
            Object rawNumber = element.get("element");
            if(rawNumber == null){
                continue;
            }

            result.add(new NumberLiteral(((Number) rawNumber).doubleValue()));
        }

        NumberLiteral[] a = result.toArray(new NumberLiteral[0]);
        return new ArrayValues<>(TypeEnum.NUMBER, a);
    }
}
