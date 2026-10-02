package at.sfischer.constraints.timeseries.driver;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataValue;
import at.sfischer.constraints.model.*;
import at.sfischer.driver.DriverException;
import at.sfischer.driver.SystemDriver;
import at.sfischer.driver.rest.RestDriverProvider;

import java.util.*;

public class ForecastPlatformMeanBatchDriver implements SystemDriver {

    private final SystemDriver driver;

    public ForecastPlatformMeanBatchDriver() {
        RestDriverProvider restDriverProvider = new RestDriverProvider();
        Map<String, Object> configValues = Map.of(
                "baseUrl", "http://localhost:8000",
                "path", "/forecast/batch/mean",
                "operation", "POST",
                "openApiSpec", "http://localhost:8000/openapi.json",
                "httpVersion", "HTTP_1_1",
                "timeoutSeconds", 600
        );

        this.driver = restDriverProvider.create(configValues);
    }

    public DataObject forecast(DataObject input) throws DriverException {
        return this.driver.execute(input);
    }

    public static List<BatchRequest> createInputs(List<DataObject> individualRequests) {
        Map<RequestKey, List<DataObject>> groupedRequests = new LinkedHashMap<>();
        for (DataObject request : individualRequests) {
            DataObject body = (DataObject) request
                    .getDataValue("forecastrequest").getValue();

            String model = body.getDataValue("model") != null
                    ? (String) body.getDataValue("model").getValue()
                    : null;

            Integer horizon = body.getDataValue("horizon") != null
                    ? (Integer) body.getDataValue("horizon").getValue()
                    : null;

            groupedRequests.computeIfAbsent(
                    new RequestKey(model, horizon),
                    k -> new ArrayList<>()
            ).add(request);
        }

        List<BatchRequest> batches = new ArrayList<>();

        for (Map.Entry<RequestKey, List<DataObject>> entry
                : groupedRequests.entrySet()) {

            RequestKey key = entry.getKey();
            List<DataObject> group = entry.getValue();

            DataObject body = new DataObject();
            body.putValue("model", key.model());
            body.putValue("horizon", key.horizon());

            List<DataValue<?>> contexts = new ArrayList<>();

            for (DataObject request : group) {
                DataObject requestBody = (DataObject) request
                        .getDataValue("forecastrequest").getValue();

                contexts.add(requestBody.getDataValue("context"));
            }

            body.putValue(
                    "contexts",
                    contexts.toArray(DataValue<?>[]::new),
                    new ArrayType(TypeEnum.NUMBER)
            );

            DataObject batchInput = new DataObject();
            batchInput.putValue("batchforecastrequest", body);

            batches.add(new BatchRequest(batchInput, group));
        }

        return batches;
    }

    private record RequestKey(String model, Integer horizon) {}

    @Override
    public String getIdentifier() {
        return "forecast-platform-batch-means";
    }

    @Override
    public DataObject execute(DataObject input) throws DriverException {
        return forecast(input);
    }
}
