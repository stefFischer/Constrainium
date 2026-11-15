package at.scch.traceon.otel.data;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataValue;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.StringLiteral;
import at.sfischer.constraints.model.Value;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.operators.array.Exists;
import at.sfischer.constraints.model.operators.logic.AndOperator;
import at.sfischer.constraints.model.operators.logic.NotOperator;
import at.sfischer.constraints.model.operators.logic.OrOperator;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOrEqualOperator;
import at.sfischer.constraints.model.operators.numbers.LessThanOrEqualOperator;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import at.sfischer.constraints.model.operators.strings.StringMatches;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.abstraction.SpanDataFinder;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class AbstractHttpSpanDataFinder extends SpanDataFinder<DataObject> {

    protected static final String TIMESTAMP = "@timestamp";
    protected static final String TIMESTAMP_US = "@timestamp_us";

    protected static final String SOURCE = "source";
    protected static final String HTTP = "http";

    protected static final String DIRECTION = "direction";
    protected static final String REQUEST = "REQUEST";
    protected static final String RESPONSE = "RESPONSE";

    protected static final String B3_SPAN_ID = "x-b3-spanid";
    protected static final String B3_TRACE_ID = "x-b3-traceid";
    protected static final String METHOD = ":method";
    protected static final String PATH = ":path";
    protected static final String AUTHORITY = ":authority";
    protected static final String STATUS = ":status";

    private static final long TIME_TOLERANCE_US = 50_000L;

    protected final DataCollection<DataObject> captures;

    protected AbstractHttpSpanDataFinder(DataCollection<DataObject> data) {
        this(withTimestamps(data), true);
    }

    private AbstractHttpSpanDataFinder(DataCollection<DataObject> enriched, boolean ignored) {
        super(enriched);
        this.captures = enriched;
    }

    protected Node httpSource() {
        return equal(SOURCE, HTTP);
    }

    protected Node headerEquals(String name, String value) {
        return new Exists(new Variable("headers"), and(
                headerName(name),
                new StringEquals(
                        new Reference(new Variable(ArrayOperation.ELEMENT_NAME), new StringLiteral("value")),
                        new StringLiteral(value)
                )
        ));
    }

    protected Node headerMatches(String name, String pattern) {
        return new Exists(new Variable("headers"), and(
                headerName(name),
                new StringMatches(
                        new Reference(new Variable(ArrayOperation.ELEMENT_NAME), new StringLiteral("value")),
                        new StringLiteral(pattern)
                )
        ));
    }

    protected Node headerAbsent(String name) {
        return new NotOperator(new Exists(new Variable("headers"), headerName(name)));
    }

    protected Node optionalHeaderEquals(String name, String value) {
        if (value == null) {
            return BooleanLiteral.TRUE;
        }
        return or(headerAbsent(name), headerEquals(name, value));
    }

    protected Node optionalHeaderMatches(String name, String pattern) {
        if (pattern == null) {
            return BooleanLiteral.TRUE;
        }
        return or(headerAbsent(name), headerMatches(name, pattern));
    }

    protected Node traceMatches(Span span) {
        return optionalHeaderEquals(B3_TRACE_ID, span.getTraceId());
    }

    protected Node statusMatches(Span span) {
        return optionalHeaderEquals(STATUS, attributeString(span, "http.response.status_code"));
    }

    protected Node endpointMatches(Span span) {
        List<Node> conditions = new ArrayList<>();

        String pathPattern = pathPattern(spanPath(span));
        if (pathPattern != null) {
            conditions.add(headerMatches(PATH, pathPattern));
        }

        if (attributeString(span, "http.request.method") != null) {
            conditions.add(optionalHeaderEquals(METHOD, attributeString(span, "http.request.method")));
        }

        String authorityPattern = authorityPattern(spanAuthorityHost(span));
        if (authorityPattern != null) {
            conditions.add(optionalHeaderMatches(AUTHORITY, authorityPattern));
        }

        return andAll(conditions);
    }

    protected Node timeWindow(Span span) {
        return and(
                new GreaterThanOrEqualOperator(
                        new Variable(TIMESTAMP_US),
                        new NumberLiteral(span.getStart() - TIME_TOLERANCE_US)
                ),
                new LessThanOrEqualOperator(
                        new Variable(TIMESTAMP_US),
                        new NumberLiteral(span.getEnd() + TIME_TOLERANCE_US)
                )
        );
    }

    protected Node andAll(List<Node> conditions) {
        if (conditions.isEmpty()) {
            return BooleanLiteral.TRUE;
        }

        Node result = conditions.get(0);
        for (int i = 1; i < conditions.size(); i++) {
            result = and(result, conditions.get(i));
        }
        return result;
    }

    protected Node orAll(List<Node> conditions) {
        if (conditions.isEmpty()) {
            return BooleanLiteral.FALSE;
        }

        Node result = conditions.get(0);
        for (int i = 1; i < conditions.size(); i++) {
            result = or(result, conditions.get(i));
        }
        return result;
    }

    protected Node or(Node left, Node right) {
        return new OrOperator(left, right);
    }

    protected DataCollection<DataObject> closestTo(DataCollection<DataObject> candidates, long reference) {
        List<DataObject> all = new ArrayList<>();
        candidates.visitDataObjects((dataObject, dataEntry) -> all.add(dataObject));

        if (all.size() <= 1) {
            return candidates;
        }

        DataObject closest = all.get(0);
        long closestDistance = distance(timestampUs(closest), reference);
        for (DataObject candidate : all) {
            long candidateDistance = distance(timestampUs(candidate), reference);
            if (candidateDistance < closestDistance) {
                closest = candidate;
                closestDistance = candidateDistance;
            }
        }

        SimpleDataCollection result = new SimpleDataCollection();
        result.addDataEntry(closest);
        return result;
    }

    protected static boolean isServer(Span span) {
        return "server".equalsIgnoreCase(span.getKind());
    }

    protected static String attributeString(Span span, String name) {
        Object value = span.getAttribute(name);
        return value == null ? null : value.toString();
    }

    protected static String spanPath(Span span) {
        String path = span.getAttribute("url.path");
        if (path != null) {
            return path;
        }

        String full = span.getAttribute("url.full");
        if (full != null) {
            try {
                String parsed = URI.create(full).getPath();
                if (parsed != null && !parsed.isEmpty()) {
                    return parsed;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return span.getAttribute("http.target");
    }

    protected static String spanAuthorityHost(Span span) {
        String full = span.getAttribute("url.full");
        if (full != null) {
            try {
                URI uri = URI.create(full);
                String host = uri.getHost();
                if (host != null && !host.isEmpty()) {
                    return host;
                }

                String authority = uri.getAuthority();
                if (authority != null && !authority.isEmpty()) {
                    return stripPort(authority);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return stripPort(span.getAttribute("server.address"));
    }

    private static Node headerName(String name) {
        return new StringEquals(
                new Reference(new Variable(ArrayOperation.ELEMENT_NAME), new StringLiteral("name")),
                new StringLiteral(name)
        );
    }

    private static String pathPattern(String spanPath) {
        if (spanPath == null || spanPath.isEmpty()) {
            return null;
        }

        int queryStart = spanPath.indexOf('?');
        String path = queryStart >= 0 ? spanPath.substring(0, queryStart) : spanPath;
        if (path.isEmpty()) {
            return null;
        }

        List<String> suffixes = new ArrayList<>();
        suffixes.add(path);
        for (int i = 1; i < path.length(); i++) {
            if (path.charAt(i) != '/') {
                continue;
            }

            String suffix = path.substring(i);
            if (!suffixes.contains(suffix)) {
                suffixes.add(suffix);
            }
        }

        StringBuilder pattern = new StringBuilder("^(?:");
        for (int i = 0; i < suffixes.size(); i++) {
            if (i > 0) {
                pattern.append('|');
            }
            pattern.append(Pattern.quote(suffixes.get(i)));
        }
        pattern.append(")(?:\\?.*)?$");
        return pattern.toString();
    }

    private static String authorityPattern(String host) {
        if (host == null || host.isEmpty()) {
            return null;
        }
        return "^" + Pattern.quote(host) + "(?::\\d+)?$";
    }

    private static String stripPort(String authority) {
        if (authority == null) {
            return null;
        }

        if (authority.startsWith("[")) {
            int end = authority.indexOf(']');
            return end >= 0 ? authority.substring(0, end + 1) : authority;
        }

        int colon = authority.lastIndexOf(':');
        if (colon > 0 && authority.indexOf(':') == colon) {
            return authority.substring(0, colon);
        }
        return authority;
    }

    private static SimpleDataCollection withTimestamps(DataCollection<DataObject> data) {
        SimpleDataCollection enriched = new SimpleDataCollection();
        data.visitDataObjects((dataObject, dataEntry) -> {
            DataObject copy = new DataObject();
            copy.putDataValues(dataObject);

            List<Value<?>> timestamps = dataObject.getValues(TIMESTAMP);
            if (timestamps != null && !timestamps.isEmpty() && timestamps.get(0) instanceof StringLiteral timestamp) {
                try {
                    Instant instant = Instant.parse(timestamp.getValue());
                    copy.putValue(TIMESTAMP_US, instant.getEpochSecond() * 1_000_000L + instant.getNano() / 1_000L);
                } catch (DateTimeParseException ignored) {
                }
            }

            enriched.addDataEntry(copy);
        });
        return enriched;
    }

    private static long timestampUs(DataObject dataObject) {
        DataValue<?> timestamp = dataObject.getDataValue(TIMESTAMP_US);
        if (timestamp == null) {
            return Long.MIN_VALUE;
        }

        Object value = timestamp.getValue();
        return value instanceof Number number ? number.longValue() : Long.MIN_VALUE;
    }

    private static long distance(long timestamp, long reference) {
        if (timestamp == Long.MIN_VALUE) {
            return Long.MAX_VALUE / 4;
        }
        return Math.abs(timestamp - reference);
    }
}
