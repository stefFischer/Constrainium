package at.sfischer.traces.otel.dataextraction.rest;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.traces.otel.TraceNode;
import at.sfischer.traces.otel.dataextraction.DataExtractor;
import at.sfischer.traces.otel.dataextraction.SpanAttributeExtractor;
import at.sfischer.traces.otel.dataextraction.SpanData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestSpanDataExtractor implements DataExtractor {

    private final SpanAttributeExtractor<String> httpRouteExtractor;
    private final SpanAttributeExtractor<String> urlPathExtractor;
    private final SpanAttributeExtractor<String> requestBodyExtractor;
    private final SpanAttributeExtractor<String> responseBodyExtractor;

    public RestSpanDataExtractor(
            SpanAttributeExtractor<String> httpRouteExtractor,
            SpanAttributeExtractor<String> urlPathExtractor,
            SpanAttributeExtractor<String> requestBodyExtractor,
            SpanAttributeExtractor<String> responseBodyExtractor) {
        this.httpRouteExtractor = httpRouteExtractor;
        this.urlPathExtractor = urlPathExtractor;
        this.requestBodyExtractor = requestBodyExtractor;
        this.responseBodyExtractor = responseBodyExtractor;
    }

    @Override
    public SpanData extractData(TraceNode<?> span) {
        String httpRoute = httpRouteExtractor.extract(span);
        String urlPath = urlPathExtractor.extract(span);
        String requestBody = requestBodyExtractor.extract(span);
        String responseBody = responseBodyExtractor.extract(span);

        return parseRestSpan(httpRoute, urlPath, requestBody, responseBody);
    }

    public static SpanData parseRestSpan(
            String httpRoute,
            String urlPath,
            String requestBody,
            String responseBody) {

        DataObject inputData = extractInput(httpRoute, urlPath, requestBody);
        DataObject outputData = extractOutput(responseBody);

        return new SpanData(
                inputData.getFieldNames().isEmpty() ? null : inputData,
                outputData
        );
    }

    // --- input ---

    private static DataObject extractInput(String httpRoute, String urlPath, String requestBody) {
        DataObject inputData = new DataObject();

        DataObject pathParams = extractPathParameters(httpRoute, urlPath);
        if (!pathParams.getFieldNames().isEmpty()) {
            inputData.putValue("path", pathParams);
        }

        if (requestBody != null) {
            DataObject body = DataObject.parseData(requestBody);
            inputData.putValue("body", body);
        }

        // TODO Add query parameter parsing.

        return inputData;
    }

    private static DataObject extractPathParameters(String httpRoute, String urlPath) {
        DataObject pathParams = new DataObject();
        if (httpRoute == null || urlPath == null) return pathParams;

        String[] routeSegments = httpRoute.split("/");
        String[] pathSegments = urlPath.split("/");

        if (routeSegments.length != pathSegments.length) return pathParams;

        for (int i = 0; i < routeSegments.length; i++) {
            String segment = routeSegments[i];
            if (segment.startsWith("{") && segment.endsWith("}")) {
                String paramName = segment.substring(1, segment.length() - 1);
                pathParams.putValue(paramName, pathSegments[i]);
            }
        }

        return pathParams;
    }

    // --- output ---

    private static DataObject extractOutput(String responseBody) {
        if (responseBody == null || responseBody.equals("[]")) return null;
        try {
            JsonNode root = new ObjectMapper().readTree(responseBody);
            if (root.isObject()) {
                return DataObject.parseData(responseBody);
            } else if (root.isArray()) {
                if (root.isEmpty()) return null;
                DataObject[] items = new DataObject[root.size()];
                for (int i = 0; i < root.size(); i++) {
                    items[i] = DataObject.parseData(root.get(i).toString());
                }
                DataObject result = new DataObject();
                result.putValue("results", items);
                return result;
            }

            return new DataObject();
        } catch (JsonProcessingException e) {
            return new DataObject();
        }
    }
}
