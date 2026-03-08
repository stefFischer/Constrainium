package at.sfischer.driver.rest;

import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.driver.DriverException;
import at.sfischer.driver.SystemDriver;
import at.sfischer.testing.systems.PayrollApplication;
import org.javatuples.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RestSystemDriverPayrollTest {

    private static PayrollApplication TEST_SYSTEM;

    @BeforeAll
    public static void setup(){
        TEST_SYSTEM = new PayrollApplication();
        TEST_SYSTEM.start();
    }

    @AfterAll
    public static void cleanup(){
        TEST_SYSTEM.stop();
    }

    @Test
    public void postEmployeeRequestBodyTest() throws DriverException {
        RestDriverProvider restDriverProvider = new RestDriverProvider();

        Map<String, Object> configValues = Map.of(
                "baseUrl", "http://localhost:8080",
                "path", "/employee",
                "operation", "POST",
                "openApiSpec", "http://localhost:8080/v3/api-docs"
        );

        SystemDriver driver = restDriverProvider.create(configValues);

        SimpleDataCollection input = SimpleDataCollection.parseData(
                "{body:{name:\"Alice\", role:\"Engineer\"}}",
                "{body:{name:\"Bob\", role:\"Manager\"}}"
        );

        InOutputDataCollection expected = InOutputDataCollection.parseData(
                new Pair<>("{body:{name:\"Alice\", role:\"Engineer\"}}", "{id:3,name:\"Alice\", role:\"Engineer\"}"),
                new Pair<>("{body:{name:\"Bob\", role:\"Manager\"}}", "{id:4,name:\"Bob\", role:\"Manager\"}")
        );

        InOutputDataCollection actual = driver.execute(input);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void putEmployeePathAndBodyTest() throws DriverException {
        RestDriverProvider restDriverProvider = new RestDriverProvider();

        Map<String, Object> configValues = Map.of(
                "baseUrl", "http://localhost:8080",
                "path", "/employee/{id}",
                "operation", "PUT",
                "openApiSpec", "http://localhost:8080/v3/api-docs"
        );

        SystemDriver driver = restDriverProvider.create(configValues);

        SimpleDataCollection input = SimpleDataCollection.parseData(
                "{id:1, body:{name:\"Alice\", role:\"Architect\"}}",
                "{id:2, body:{name:\"Bob\", role:\"Director\"}}"
        );

        InOutputDataCollection expected = InOutputDataCollection.parseData(
                new Pair<>("{id:1, body:{name:\"Alice\", role:\"Architect\"}}", "{id:1, name:\"Alice\", role:\"Architect\"}"),
                new Pair<>("{id:2, body:{name:\"Bob\", role:\"Director\"}}", "{id:2, name:\"Bob\", role:\"Director\"}")
        );

        InOutputDataCollection actual = driver.execute(input);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void noParameterEndpointTest() throws DriverException {
        RestDriverProvider restDriverProvider = new RestDriverProvider();

        Map<String, Object> configValues = Map.of(
                "baseUrl", "http://localhost:8080",
                "path", "/employees",
                "operation", "GET",
                "openApiSpec", "http://localhost:8080/v3/api-docs"
        );

        SystemDriver driver = restDriverProvider.create(configValues);

        SimpleDataCollection input = SimpleDataCollection.parseData(
                "{}"
        );

        InOutputDataCollection actual = driver.execute(input);

        assertThat(actual).isNotNull();
    }

    @Test
    public void deleteEmployeeTest() throws DriverException {
        RestDriverProvider restDriverProvider = new RestDriverProvider();

        Map<String, Object> configValues = Map.of(
                "baseUrl", "http://localhost:8080",
                "path", "/employee/{id}",
                "operation", "DELETE",
                "openApiSpec", "http://localhost:8080/v3/api-docs"
        );

        SystemDriver driver = restDriverProvider.create(configValues);

        SimpleDataCollection input = SimpleDataCollection.parseData(
                "{id:2}"
        );

        InOutputDataCollection actual = driver.execute(input);

        assertThat(actual).isNotNull();
    }
}
