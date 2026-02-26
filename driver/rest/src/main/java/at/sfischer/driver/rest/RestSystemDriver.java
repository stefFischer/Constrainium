package at.sfischer.driver.rest;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.driver.DriverException;
import at.sfischer.driver.SystemDriver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RestSystemDriver implements SystemDriver {

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

        input.visitDataEntries((values, dataEntry) -> {
            if(!(dataEntry instanceof DataObject dataObject)){
                return;
            }

            /// Get the parameters.
            String resolvedPath = pathTemplate;
            Map<String, String> queryParams = new LinkedHashMap<>();
            Map<String, String> headers = new LinkedHashMap<>();
            Map<String, String> cookies = new LinkedHashMap<>();
            for (Parameter p : operation.getParameters()) {

                String name = p.getName();
                Object rawValue = dataObject.getDataValue(name).getValue();

                // Skip null optional parameters
                if (rawValue == null) {

                    if (Boolean.TRUE.equals(p.getRequired())) {
                        // TODO
//                        throw new DriverException("Missing required parameter: " + name);
                    }

                    continue;
                }

                String value = rawValue.toString();

                switch (p.getIn()) {

                    case "path" -> {
                        resolvedPath = resolvedPath.replace(
                                "{" + name + "}", value);
                    }

                    case "query" -> {
                        queryParams.put(name, value);
                    }

                    case "header" -> {
                        headers.put(name, value);
                    }

                    case "cookie" -> {
                        cookies.put(name, value);
                    }

//                    default -> throw new DriverException("Unsupported parameter location: " + p.getIn());
                }
            }


            RequestBody body = operation.getRequestBody();
            if (body != null) {
                Schema<?> schema = body.getContent()
                        .get("application/json")
                        .getSchema();

                // build JSON from schema
            }

            String jsonBody = "";

            ///  Build URI.
            StringBuilder uriBuilder = new StringBuilder();
            uriBuilder.append(uri.toString());
            uriBuilder.append(resolvedPath);

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
                case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));

                default -> throw new IllegalArgumentException("Unsupported operation, needs to be one of GET, POST, PUT, PATCH, DELETE.");
            };

            headers.forEach(builder::header);
            if (!cookies.isEmpty()) {

                String cookieHeader = cookies.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("; "));

                builder.header("Cookie", cookieHeader);
            }

            HttpRequest request = builder.build();
            HttpClient client = HttpClient.newHttpClient();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println(response);
                System.out.println(response.statusCode());
                System.out.println(response.body());
                // Convert Response → DataCollection

                // TODO is there a nicer way to check if the status code in in the 200 family?
                if(response.statusCode() == 200){
                    DataObject responseBody = DataObject.parseData(response.body());


                    inout.addDataEntry(dataObject, responseBody);
                }
                // TODO special handing for error cases. 

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });


        return inout;
    }
}
