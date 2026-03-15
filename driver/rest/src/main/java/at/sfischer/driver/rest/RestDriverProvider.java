package at.sfischer.driver.rest;

import at.sfischer.ConfigurationDescriptor;
import at.sfischer.ConfigurationField;
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
    public ConfigurationDescriptor getConfigurationDescriptor() {
        return new ConfigurationDescriptor(List.of(
                new ConfigurationField(
                        "baseUrl",
                        String.class,
                        true,
                        "Base URL of the REST service"),

                new ConfigurationField(
                        "openApiSpec",
                        String.class,
                        true,
                        "Path to OpenAPI specification file"),

                new ConfigurationField(
                        "path",
                        String.class,
                        true,
                        "Operation ID of the endpoint to call"),

                new ConfigurationField(
                        "operation",
                        String.class,
                        true,
                        "Operation of the endpoint to call (GET, POST, PUT, PATCH, DELETE)"),

                new ConfigurationField(
                        "timeoutSeconds",
                        Integer.class,
                        false,
                        "HTTP timeout in seconds")
        ));
    }

    @Override
    public RestSystemDriver create(Map<String, Object> configValues) {
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
