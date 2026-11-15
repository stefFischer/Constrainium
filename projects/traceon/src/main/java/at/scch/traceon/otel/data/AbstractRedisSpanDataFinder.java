package at.scch.traceon.otel.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataValue;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.StringLiteral;
import at.sfischer.constraints.model.Value;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.array.ArrayOperation;
import at.sfischer.constraints.model.operators.array.Exists;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOrEqualOperator;
import at.sfischer.constraints.model.operators.numbers.LessThanOrEqualOperator;
import at.sfischer.constraints.model.operators.objects.Reference;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.abstraction.SpanDataFinder;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractRedisSpanDataFinder extends SpanDataFinder<DataObject> {

    private static final String TIMESTAMP = "@timestamp";
    private static final String TIMESTAMP_US = "@timestamp_us";
    private static final String CONNECTION_ID = "attributes.envoy.connection_id";

    protected AbstractRedisSpanDataFinder(DataCollection<DataObject> data) {
        super(withTimestamps(data));
    }

    protected abstract String direction();

    @Override
    protected Constraint createConstraint(Span span) {
        Node inTimeWindow = and(
                new GreaterThanOrEqualOperator(
                        new Variable(TIMESTAMP_US),
                        new NumberLiteral(span.getStart())
                ),
                new LessThanOrEqualOperator(
                        new Variable(TIMESTAMP_US),
                        new NumberLiteral(span.getEnd())
                )
        );

        Node captureDirection = equal("direction", direction());

        if (isPipeline(span)) {
            return new Constraint(and(captureDirection, inTimeWindow));
        }

        return new Constraint(and(captureDirection, and(
                inTimeWindow,
                headerEquals("redis.command", operation(span))
        )));
    }

    @Override
    public DataCollection<DataObject> findData(Span span) {
        DataCollection<DataObject> matches = super.findData(span);
        if (!isPipeline(span)) {
            return matches;
        }

        List<DataObject> candidates = new ArrayList<>();
        matches.visitDataObjects((dataObject, dataEntry) -> candidates.add(dataObject));

        List<DataObject> selected = selectPipeline(candidates, batchSize(span));
        SimpleDataCollection result = new SimpleDataCollection();
        selected.forEach(result::addDataEntry);
        return result;
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
                    // No usable timestamp, the entry cannot match the time window.
                }
            }

            enriched.addDataEntry(copy);
        });
        return enriched;
    }

    private Node headerEquals(String name, String value) {
        return new Exists(new Variable("headers"), and(
                new StringEquals(
                        new Reference(new Variable(ArrayOperation.ELEMENT_NAME), new StringLiteral("name")),
                        new StringLiteral(name)
                ),
                new StringEquals(
                        new Reference(new Variable(ArrayOperation.ELEMENT_NAME), new StringLiteral("value")),
                        new StringLiteral(value)
                )
        ));
    }

    private static List<DataObject> selectPipeline(List<DataObject> candidates, int batchSize) {
        Map<String, List<DataObject>> byConnection = new LinkedHashMap<>();
        for (DataObject candidate : candidates) {
            byConnection.computeIfAbsent(connectionId(candidate), key -> new ArrayList<>()).add(candidate);
        }

        List<DataObject> best = List.of();
        for (List<DataObject> connectionCandidates : byConnection.values()) {
            List<DataObject> selected = selectPipelineBlock(connectionCandidates, batchSize);
            if (best.isEmpty() || score(selected, batchSize) > score(best, batchSize)) {
                best = selected;
            }
        }

        return best;
    }

    private static List<DataObject> selectPipelineBlock(List<DataObject> candidates, int batchSize) {
        List<DataObject> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator.comparingLong(AbstractRedisSpanDataFinder::timestampUs));

        int multi = -1;
        int exec = -1;
        for (int i = 0; i < ordered.size(); i++) {
            String command = command(ordered.get(i));
            if ("MULTI".equalsIgnoreCase(command)) {
                multi = i;
                exec = -1;
            } else if ("EXEC".equalsIgnoreCase(command) && multi >= 0) {
                exec = i;
            }
        }

        if (multi >= 0 && exec > multi) {
            List<DataObject> block = ordered.subList(multi, exec + 1);
            if (batchSize <= 0 || block.size() == batchSize) {
                return block;
            }
            if (ordered.size() == batchSize) {
                return ordered;
            }
            return block;
        }

        return ordered;
    }

    private static int score(List<DataObject> selected, int batchSize) {
        int score = selected.size();
        if (batchSize > 0 && selected.size() == batchSize) {
            score += 1000;
        }
        if (selected.stream().anyMatch(candidate -> "MULTI".equalsIgnoreCase(command(candidate)))) {
            score += 100;
        }
        return score;
    }

    private static boolean isPipeline(Span span) {
        return "PIPELINE".equalsIgnoreCase(operation(span));
    }

    private static String operation(Span span) {
        String operation = span.getAttribute("db.operation.name");
        return operation == null ? "" : operation;
    }

    private static int batchSize(Span span) {
        Object batchSize = span.getAttribute("db.operation.batch.size");
        if (batchSize instanceof Number number) {
            return number.intValue();
        }
        if (batchSize != null) {
            try {
                return Integer.parseInt(batchSize.toString());
            } catch (NumberFormatException ignored) {
                // No usable batch size, fall back to the commands found in the time window.
            }
        }
        return -1;
    }

    private static String connectionId(DataObject dataObject) {
        String connectionId = stringValue(dataObject, CONNECTION_ID);
        if (connectionId == null) {
            connectionId = stringValue(dataObject, "trace_id");
        }
        return connectionId == null ? "" : connectionId;
    }

    private static String command(DataObject dataObject) {
        return headerValue(dataObject, "redis.command");
    }

    private static String headerValue(DataObject dataObject, String name) {
        DataValue<?> headersValue = dataObject.getDataValue("headers");
        if (headersValue == null || !(headersValue.getValue() instanceof DataObject[] headers)) {
            return null;
        }

        for (DataObject header : headers) {
            String headerName = stringValue(header, "name");
            if (name.equalsIgnoreCase(headerName)) {
                return stringValue(header, "value");
            }
        }

        return null;
    }

    private static String stringValue(DataObject dataObject, String path) {
        List<Value<?>> values = dataObject.getValues(path);
        if (values == null || values.isEmpty()) {
            return null;
        }

        Value<?> value = values.get(0);
        return value instanceof StringLiteral stringLiteral ? stringLiteral.getValue() : null;
    }

    private static long timestampUs(DataObject dataObject) {
        DataValue<?> timestamp = dataObject.getDataValue(TIMESTAMP_US);
        if (timestamp == null) {
            return Long.MIN_VALUE;
        }

        Object value = timestamp.getValue();
        return value instanceof Number number ? number.longValue() : Long.MIN_VALUE;
    }
}
