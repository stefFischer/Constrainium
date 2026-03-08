package at.sfischer.driver.rest;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataValue;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.driver.DriverException;
import at.sfischer.driver.SystemDriver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RestSystemDriver implements SystemDriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestSystemDriver.class);

    private final OpenAPI openAPI;
    private final URI uri;
    private final String pathTemplate;
    private final String operation;
    private final Duration duration;

    public RestSystemDriver(OpenAPI openAPI, URI uri, String path, String operation, Duration duration) {
        this.openAPI = openAPI;
        this.uri = uri;
        this.pathTemplate = path;
        this.operation = operation;
        this.duration = duration;
    }

    @Override
    public String getIdentifier() {
        return RestDriverProvider.DRIVER_NAME;
    }

    @Override
    public InOutputDataCollection execute(SimpleDataCollection input) throws DriverException {

        InOutputDataCollection inout = new InOutputDataCollection();

        PathItem pathItem = openAPI.getPaths().get(pathTemplate);
        Operation operation = switch (this.operation){
            case "GET" -> pathItem.getGet();
            case "POST" -> pathItem.getPost();
            case "PUT" -> pathItem.getPut();
            case "PATCH" -> pathItem.getPatch();
            case "DELETE" -> pathItem.getDelete();
            default -> throw new IllegalArgumentException("Unsupported operation, needs to be one of GET, POST, PUT, PATCH, DELETE.");
        };

        for (DataObject dataEntry : input.getDataCollection()) {
            if(!(dataEntry instanceof DataObject dataObject)){
                continue;
            }

            /// Get the parameters.
            String resolvedPath = pathTemplate;
            Map<String, String> queryParams = new LinkedHashMap<>();
            Map<String, String> headers = new LinkedHashMap<>();
            Map<String, String> cookies = new LinkedHashMap<>();
            List<Parameter> parameters = operation.getParameters();
            if(parameters != null){
                for (Parameter p : parameters) {

                    String name = p.getName();
                    Object rawValue = dataObject.getDataValue(name).getValue();

                    // Skip null optional parameters
                    if (rawValue == null) {
                        if (Boolean.TRUE.equals(p.getRequired())) {
                            throw new DriverException("Missing required parameter: " + name);
                        }

                        continue;
                    }

                    String value = rawValue.toString();
                    switch (p.getIn()) {
                        case "path" -> resolvedPath = resolvedPath.replace("{" + name + "}", value);
                        case "query" -> queryParams.put(name, value);
                        case "header" -> headers.put(name, value);
                        case "cookie" -> cookies.put(name, value);
                        default -> throw new DriverException("Unsupported parameter location: " + p.getIn());
                    }
                }
            }

            /// Build request body.
            RequestBody body = operation.getRequestBody();
            String requestBody = "";
            String contentType = "";
            if (body != null) {
                contentType = body
                        .getContent()
                        .keySet()
                        .iterator()
                        .next();

                Schema<?> schema = body.getContent()
                        .get(contentType)
                        .getSchema();

                switch (contentType) {
                    case "application/json": {
                        DataValue<?> rootValue = dataEntry.getDataValue("body");
                        Object jsonObject = buildJsonFromSchema(schema, rootValue);
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            requestBody = mapper.writeValueAsString(jsonObject);
                        } catch (JsonProcessingException e) {
                            throw new DriverException("Error while trying to build request body", e);
                        }
                        break;
                    }
                    case "application/xml":
                    case "application/x-www-form-urlencoded":
                    case "application/octet-stream":
                    case "multipart/form-data":
                    default: throw new DriverException("Unsupported content type for request body: " + contentType);
                }
            }

            ///  Build URI.
            StringBuilder uriBuilder = new StringBuilder();
            uriBuilder.append(uri.toString());
            uriBuilder.append(resolvedPath);

            ///  - Append query parameters
            if (!queryParams.isEmpty()) {
                uriBuilder.append("?");
                boolean first = true;
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    if (!first) {
                        uriBuilder.append("&");
                    }
                    first = false;

                    uriBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                    uriBuilder.append("=");
                    uriBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                }
            }

            URI finalUri = URI.create(uriBuilder.toString());

            /// Build request.
            HttpRequest.Builder builder =
                    HttpRequest.newBuilder()
                            .uri(finalUri)
                            .timeout(duration);


            switch (this.operation){
                case "GET" -> builder.GET();
                case "DELETE" -> builder.DELETE();
                case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(requestBody));
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(requestBody));

                default -> throw new IllegalArgumentException("Unsupported operation, needs to be one of GET, POST, PUT, PATCH, DELETE.");
            }

            ///  - Add headers and cookies.
            headers.forEach(builder::header);
            if (!cookies.isEmpty()) {
                String cookieHeader = cookies.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("; "));

                builder.header("Cookie", cookieHeader);
            }

            HttpRequest request;
            if(requestBody.isEmpty()){
                request = builder.build();
            } else {
                request = builder.header("Content-Type", contentType).build();
            }

            LOGGER.debug("Created request: {} with body: {}", request, requestBody);

            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                LOGGER.debug("Response: {} with body: {}", response, response.body());

                // Convert Response → DataCollection
                if(isSuccess(response)){
                    String b = response.body();
                    if(b == null || b.isEmpty()){
                        b = "{}";
                    }
                    DataObject responseBody = DataObject.parseData(b);
                    inout.addDataEntry(dataObject, responseBody);
                } else {
                    // TODO special handing for error cases.
                    LOGGER.debug("Error ({}) \"{}\" for data: {}", response.statusCode(), response.body(), dataObject);
                }


            } catch (Exception e) {
                throw new DriverException(e);
            }
        }

        return inout;
    }

    private boolean isSuccess(HttpResponse<?> response) {
        return response.statusCode() / 100 == 2;
    }

    private Object buildJsonFromSchema(Schema<?> schema, DataValue<?> value) throws DriverException {
        schema = resolveSchema(schema, openAPI);

        if (schema == null) {
            return null;
        }

        Object raw = value.getValue();
        if (raw == null) {
            return null;
        }

        return switch (schema.getType()) {
            case "object" -> {
                if (!(raw instanceof DataObject obj)) {
                    throw new DriverException("Expected object for schema");
                }
                yield buildJsonObject(schema, obj);
            }
            case "array" -> {
                if (!(raw instanceof List<?> list)) {
                    throw new DriverException("Expected array for schema");
                }
                yield buildJsonArray(schema, list);
            }
            default -> raw;
        };
    }

    private Schema<?> resolveSchema(Schema<?> schema, OpenAPI openAPI) {
        if (schema == null) return null;

        while (schema.get$ref() != null) {
            String ref = schema.get$ref();
            String name = ref.substring(ref.lastIndexOf("/") + 1);
            schema = openAPI.getComponents().getSchemas().get(name);
        }

        return schema;
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Object> buildJsonObject(Schema<?> schema, DataObject dataObject) throws DriverException {
        Map<String, Object> json = new LinkedHashMap<>();
        Map<String, Schema> properties = schema.getProperties();
        if (properties == null) {
            return json;
        }

        for (Map.Entry<String, Schema> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            Schema<?> propertySchema = entry.getValue();
            DataValue<?> dataValue = dataObject.getDataValue(propertyName);
            if (dataValue == null) {
                continue;
            }

            Object value = buildJsonFromSchema(propertySchema, dataValue);
            json.put(propertyName, value);
        }

        return json;
    }

    private List<Object> buildJsonArray(Schema<?> schema, List<?> list) throws DriverException {
        List<Object> jsonArray = new ArrayList<>();
        Schema<?> itemSchema = schema.getItems();
        for (Object element : list) {
            if (element instanceof DataValue<?> dv) {
                jsonArray.add(buildJsonFromSchema(itemSchema, dv));
            } else if (element instanceof DataObject obj) {
                jsonArray.add(buildJsonObject(itemSchema, obj));
            } else {
                jsonArray.add(element);
            }
        }

        return jsonArray;
    }
}
