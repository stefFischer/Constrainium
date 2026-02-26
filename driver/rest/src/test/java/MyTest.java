
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataSchema;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.driver.DriverException;
import at.sfischer.driver.SystemDriver;
import at.sfischer.driver.rest.RestDriverProvider;
import at.sfischer.driver.rest.RestSystemDriver;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class MyTest {

    @Test
    public void test() throws DriverException {
        RestDriverProvider restDriverProvider = new RestDriverProvider();

        Map<String, Object> configValues = Map.of(
                "baseUrl", "http://localhost:8080",
                "path", "/api/bessj/{n}/{x}",
                "operation", "GET",
                "openApiSpec", "http://localhost:8080/v2/api-docs"
        );

        SystemDriver driver = restDriverProvider.create(configValues);

        SimpleDataCollection input = SimpleDataCollection.parseData(
                "{n:0, x:0.5}",
                "{n:3, x:0.5}",
                "{n:4, x:10.0}",
                "{n:5, x:1.2}",
                "{n:6, x:7.7}",
                "{n:8, x:15.0}",
                "{n:10, x:5.0}"
        );

        DataCollection<?> results = driver.execute(input);

        results.visitDataEntries((values, dataEntry) -> {
            System.out.println(dataEntry);
        });

        DataSchema schema = results.deriveSchema();

        System.out.println(schema);
    }
}
