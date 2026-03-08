package at.sfischer.driver.rest;

import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.driver.DriverException;
import at.sfischer.driver.SystemDriver;
import at.sfischer.testing.systems.EMBNcs;
import org.javatuples.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RestSystemDriverEMBNcsTest {

    private static EMBNcs TEST_SYSTEM;

    @BeforeAll
    public static void setup(){
        TEST_SYSTEM = new EMBNcs();
        TEST_SYSTEM.start();
    }

    @AfterAll
    public static void cleanup(){
        TEST_SYSTEM.stop();
    }

    @Test
    public void pathParameterTest() throws DriverException {
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

        InOutputDataCollection expected = InOutputDataCollection.parseData(
                new Pair<>("{x:0.5, n:3}", "{resultAsDouble:0.0025637299945872444}"),
                new Pair<>("{x:10.0, n:4}", "{resultAsDouble:-0.2196026861793646}"),
                new Pair<>("{x:1.2, n:5}", "{resultAsDouble:0.0006101049237489683}"),
                new Pair<>("{x:7.7, n:6}", "{resultAsDouble:0.3515694898409193}"),
                new Pair<>("{x:15.0, n:8}", "{resultAsDouble:-0.17398365912227323}"),
                new Pair<>("{x:5.0, n:10}", "{resultAsDouble:0.001467802647310474}")
        );

        InOutputDataCollection actual = driver.execute(input);

        assertThat(actual).
                usingRecursiveComparison().
                isEqualTo(expected);
    }
}
