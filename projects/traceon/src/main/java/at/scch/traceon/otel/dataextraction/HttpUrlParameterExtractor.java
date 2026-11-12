package at.scch.traceon.otel.dataextraction;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.traces.otel.Span;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Extracts path and query parameters from HTTP span URL attributes.
 * <p>
 * Supports both Spring-style ({param}) and Express-style (:param) route patterns.
 * Resolves the actual path from url.path (server spans) or url.full (client spans).
 * <p>
 * When no route pattern is available (or it does not match the path), ID-like
 * segments (UUIDs or segments containing both digits and uppercase letters) are
 * detected heuristically and named after the preceding segment
 * (e.g. /products/STRW-001 → productId).
 */
public class HttpUrlParameterExtractor {

    private static final String PATH_PARAMS = "path_params";
    private static final String QUERY = "query";
    private static final String URL_PATH = "url.path";
    private static final String URL_FULL = "url.full";
    private static final String HTTP_ROUTE = "http.route";

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /**
     * Extracts path and query parameters from an HTTP span.
     *
     * @return a DataObject with nested "path_params.{name}" and/or "query.{name}" entries,
     *         or null if no parameters could be extracted.
     */
    public DataObject extract(Span span) {
        DataObject result = new DataObject();

        String path = resolvePath(span);
        String route = resolveRoute(span);
        String queryString = resolveQueryString(span);

        Map<String, String> pathParams = null;
        if (path != null && route != null) {
            pathParams = extractPathParams(path, route);
        }
        if (pathParams == null || pathParams.isEmpty()) {
            pathParams = extractHeuristicPathParams(path);
        }
        for (Map.Entry<String, String> entry : pathParams.entrySet()) {
            result.putValue(PATH_PARAMS + "." + entry.getKey(), entry.getValue());
        }

        if (queryString != null && !queryString.isEmpty()) {
            Map<String, String> queryParams = parseQueryString(queryString);
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                result.putValue(QUERY + "." + entry.getKey(), entry.getValue());
            }
        }

        return result.getFieldNames().isEmpty() ? null : result;
    }

    private String resolvePath(Span span) {
        String urlPath = span.getAttribute(URL_PATH);
        if (urlPath != null && !urlPath.isEmpty()) {
            int queryIndex = urlPath.indexOf('?');
            return queryIndex >= 0 ? urlPath.substring(0, queryIndex) : urlPath;
        }

        String urlFull = span.getAttribute(URL_FULL);
        if (urlFull != null && !urlFull.isEmpty()) {
            return extractPathFromUrl(urlFull);
        }

        return null;
    }

    private String resolveRoute(Span span) {
        String route = span.getAttribute(HTTP_ROUTE);
        if (route != null && !route.isEmpty()) {
            return route;
        }

        String name = span.getName();
        if (name != null) {
            String path = extractPathFromSpanName(name);
            if (path != null && containsPattern(path)) {
                return path;
            }
        }

        return null;
    }

    private String resolveQueryString(Span span) {
        String urlFull = span.getAttribute(URL_FULL);
        if (urlFull != null) {
            int queryIndex = urlFull.indexOf('?');
            if (queryIndex >= 0 && queryIndex < urlFull.length() - 1) {
                return urlFull.substring(queryIndex + 1);
            }
        }

        String urlPath = span.getAttribute(URL_PATH);
        if (urlPath != null) {
            int queryIndex = urlPath.indexOf('?');
            if (queryIndex >= 0 && queryIndex < urlPath.length() - 1) {
                return urlPath.substring(queryIndex + 1);
            }
        }

        return null;
    }

    private String extractPathFromUrl(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return url;
        }

        int pathStart = url.indexOf('/', schemeEnd + 3);
        if (pathStart < 0) {
            return "/";
        }

        String pathAndQuery = url.substring(pathStart);
        int queryIndex = pathAndQuery.indexOf('?');
        return queryIndex >= 0 ? pathAndQuery.substring(0, queryIndex) : pathAndQuery;
    }

    private String extractPathFromSpanName(String name) {
        int pathStart = name.indexOf('/');
        if (pathStart < 0) {
            return null;
        }
        return name.substring(pathStart);
    }

    private boolean containsPattern(String path) {
        if (path.contains("{")) {
            return true;
        }
        // Match :param segments (e.g. /carts/:cartId) but not :port in URLs
        for (String segment : splitPath(path)) {
            if (segment.startsWith(":")) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> extractPathParams(String path, String route) {
        Map<String, String> params = new LinkedHashMap<>();

        String[] pathSegments = splitPath(path);
        String[] routeSegments = splitPath(route);

        if (pathSegments.length != routeSegments.length) {
            return params;
        }

        for (int i = 0; i < pathSegments.length; i++) {
            String paramName = extractParamName(routeSegments[i]);
            if (paramName != null) {
                params.put(paramName, urlDecode(pathSegments[i]));
            }
        }

        return params;
    }

    /**
     * Fallback used when no route pattern is available (or it does not match the path):
     * detects ID-like segments and names them after the preceding segment.
     */
    private Map<String, String> extractHeuristicPathParams(String path) {
        Map<String, String> params = new LinkedHashMap<>();
        if (path == null) {
            return params;
        }

        String[] segments = splitPath(path);
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (!isIdLike(segment)) {
                continue;
            }

            String name = deriveParamName(segments, i);
            if (!params.containsKey(name)) {
                params.put(name, urlDecode(segment));
            }
        }

        return params;
    }

    private boolean isIdLike(String segment) {
        if (UUID_PATTERN.matcher(segment).matches()) {
            return true;
        }

        boolean hasDigit = false;
        boolean hasUpper = false;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isUpperCase(c)) {
                hasUpper = true;
            }
        }
        return hasDigit && hasUpper;
    }

    private String deriveParamName(String[] segments, int index) {
        if (index > 0 && !isIdLike(segments[index - 1])) {
            return singularize(camelCase(segments[index - 1])) + "Id";
        }
        return "param" + index;
    }

    /**
     * Naive singularization so /carts/{id} → cartId (matching the route-based
     * naming) instead of cartsId.
     */
    private String singularize(String word) {
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    private String camelCase(String segment) {
        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c == '_' || c == '-') {
                upperNext = true;
            } else if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String[] splitPath(String path) {
        String trimmed = path.replaceAll("^/+", "").replaceAll("/+$", "");
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        return trimmed.split("/");
    }

    private String extractParamName(String segment) {
        if (segment.startsWith("{") && segment.endsWith("}")) {
            return segment.substring(1, segment.length() - 1);
        }
        if (segment.startsWith(":")) {
            return segment.substring(1);
        }
        return null;
    }

    private Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new LinkedHashMap<>();

        for (String pair : queryString.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }

            int eqIndex = pair.indexOf('=');
            if (eqIndex >= 0) {
                String key = urlDecode(pair.substring(0, eqIndex));
                String value = urlDecode(pair.substring(eqIndex + 1));
                params.put(key, value);
            } else {
                params.put(urlDecode(pair), "");
            }
        }

        return params;
    }

    private String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
