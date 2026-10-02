package at.sfischer.constraints.timeseries.driver;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.ArrayValues;
import at.sfischer.constraints.model.IntegerLiteral;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.driver.DriverException;
import at.sfischer.driver.SystemDriver;
import at.sfischer.driver.rest.RestDriverProvider;

import java.util.Map;

public class ForecastPlatformMeanDriver implements SystemDriver {

    private final SystemDriver driver;

    public ForecastPlatformMeanDriver() {
        RestDriverProvider restDriverProvider = new RestDriverProvider();
        Map<String, Object> configValues = Map.of(
                "baseUrl", "http://localhost:8000",
                "path", "/forecast/mean",
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

    public DataObject forecast(String model, ArrayValues<NumberLiteral> context, IntegerLiteral horizon) throws DriverException {
        DataObject input = createInput(model, context, horizon);
        return forecast(input);
    }

    public static DataObject createInput(String model, ArrayValues<NumberLiteral> context, IntegerLiteral horizon){
        DataObject input = new DataObject();
        DataObject body = new DataObject();
        body.putValue("model", model);
        body.putNodeValue("context", context);
        body.putNodeValue("horizon", horizon);
        input.putValue("forecastrequest", body);
        return input;
    }

    public static DataObject createInput(String model, ArrayValues<NumberLiteral> context, IntegerLiteral horizon, ArrayValues<NumberLiteral> groundTruth){
        DataObject input = new DataObject();
        DataObject body = new DataObject();
        body.putValue("model", model);
        body.putNodeValue("context", context);
        body.putNodeValue("horizon", horizon);
        body.putNodeValue("groundTruth", groundTruth);
        input.putValue("forecastrequest", body);
        return input;
    }

    @Override
    public String getIdentifier() {
        return "forecast-platform-mean";
    }

    @Override
    public DataObject execute(DataObject input) throws DriverException {
        return forecast(input);
    }
}
