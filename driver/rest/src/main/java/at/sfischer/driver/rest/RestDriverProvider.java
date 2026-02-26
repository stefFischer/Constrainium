package at.sfischer.driver.rest;

import at.sfischer.driver.DriverConfigurationDescriptor;
import at.sfischer.driver.DriverConfigurationField;
import at.sfischer.driver.SystemDriver;
import at.sfischer.driver.SystemDriverProvider;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class RestDriverProvider implements SystemDriverProvider {

    public static final String DRIVER_NAME = "rest";

    @Override
    public String getIdentifier() {
        return DRIVER_NAME;
    }

    @Override
    public DriverConfigurationDescriptor getConfigurationDescriptor() {
        return new DriverConfigurationDescriptor(List.of(
                new DriverConfigurationField(
                        "baseUrl",
                        String.class,
                        true,
                        "Base URL of the REST service"),

                new DriverConfigurationField(
                        "openApiSpec",
                        String.class,
                        true,
                        "Path to OpenAPI specification file"),

                new DriverConfigurationField(
                        "path",
                        String.class,
                        true,
                        "Operation ID of the endpoint to call"),

                new DriverConfigurationField(
                        "operation",
                        String.class,
                        true,
                        "Operation of the endpoint to call (GET, POST, PUT, PATCH, DELETE)"),

                new DriverConfigurationField(
                        "timeoutSeconds",
                        Integer.class,
                        false,
                        "HTTP timeout in seconds")
        ));
    }

    @Override
    public SystemDriver create(Map<String, Object> configValues) {
        String baseUrl = (String) configValues.get("baseUrl");
        String specPath = (String) configValues.get("openApiSpec");
        String operation = (String) configValues.get("operation");
        String path = (String) configValues.get("path");
        Integer timeout = (Integer) configValues.getOrDefault("timeoutSeconds", 5);

        OpenAPI openAPI = new OpenAPIV3Parser().read(specPath);
        return new RestSystemDriver(
                openAPI,
                URI.create(baseUrl),
                path,
                operation,
                Duration.ofSeconds(timeout));
    }
}
