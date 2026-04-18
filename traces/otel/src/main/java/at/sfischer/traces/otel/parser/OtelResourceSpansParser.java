package at.sfischer.traces.otel.parser;

import at.sfischer.traces.otel.Attributes;
import at.sfischer.traces.otel.Span;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

public class OtelResourceSpansParser implements TraceParser {

    private final JsonFactory factory;
    private final ObjectMapper mapper;

    public OtelResourceSpansParser() {
        this.factory = new JsonFactory();
        this.mapper = new ObjectMapper(factory);
    }

    @Override
    public List<Span> parse(InputStream inputStream) {
        return parse(new InputStreamReader(inputStream));
    }

    @Override
    public List<Span> parse(Reader reader) {
        List<Span> spans = new LinkedList<>();

        try {
            JsonParser parser = factory.createParser(reader);
            JsonNode root = mapper.readTree(parser);

            JsonNode resourceSpans = root.get("resourceSpans");
            if (resourceSpans == null || !resourceSpans.isArray()) {
                throw new RuntimeException("Expected 'resourceSpans' array");
            }

            for (JsonNode resourceSpan : resourceSpans) {

                JsonNode scopeSpans = resourceSpan.get("scopeSpans");
                if (scopeSpans == null) continue;

                for (JsonNode scopeSpan : scopeSpans) {

                    String tracer = null;
                    JsonNode scope = scopeSpan.get("scope");
                    if (scope != null) {
                        tracer = scope.path("name").asText(null);
                    }

                    JsonNode spanNodes = scopeSpan.get("spans");
                    if (spanNodes == null || !spanNodes.isArray()) continue;

                    for (JsonNode spanNode : spanNodes) {

                        String name = spanNode.path("name").asText();
                        String traceId = spanNode.path("traceId").asText();
                        String spanId = spanNode.path("spanId").asText();
                        String parentSpanId = spanNode.path("parentSpanId").asText(null);

                        long startTime = spanNode.path("startTimeUnixNano").asLong();
                        long endTime = spanNode.path("endTimeUnixNano").asLong();

                        String kind = mapKind(spanNode.path("kind").asInt());

                        Attributes attributes = new Attributes();
                        extractAttributes(spanNode.get("attributes"), attributes);

                        Span span = new Span(
                                name,
                                spanId,
                                traceId,
                                parentSpanId,
                                kind,
                                tracer,
                                startTime,
                                endTime
                        );

                        span.putAttributes(attributes);
                        spans.add(span);
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return spans;
    }

    private void extractAttributes(JsonNode attrNodes, Attributes attributes) {
        if (attrNodes == null || !attrNodes.isArray()) return;

        for (JsonNode attr : attrNodes) {
            String key = attr.path("key").asText();
            JsonNode valueNode = attr.path("value");

            if (valueNode.has("stringValue")) {
                attributes.put(key, valueNode.get("stringValue").asText());
            } else if (valueNode.has("intValue")) {
                attributes.put(key, valueNode.get("intValue").asLong());
            } else if (valueNode.has("boolValue")) {
                attributes.put(key, valueNode.get("boolValue").asBoolean());
            }
            // ignore other types for now (arrays, doubles, etc.)
        }
    }

    private String mapKind(int kind) {
        return switch (kind) {
            case 1 -> "INTERNAL";
            case 2 -> "SERVER";
            case 3 -> "CLIENT";
            case 4 -> "PRODUCER";
            case 5 -> "CONSUMER";
            default -> null;
        };
    }
}