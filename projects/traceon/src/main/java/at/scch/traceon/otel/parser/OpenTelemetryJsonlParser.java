package at.scch.traceon.otel.parser;

import at.sfischer.traces.otel.Attributes;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.collector.TraceListener;
import at.sfischer.traces.otel.parser.TraceParser;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OpenTelemetryJsonlParser implements TraceParser {

    private final JsonFactory factory;
    private final ObjectMapper mapper;

    public OpenTelemetryJsonlParser() {
        this.factory = new JsonFactory();
        this.mapper = new ObjectMapper(factory);
    }

    @Override
    public void parse(InputStream inputStream, TraceListener listener) {
        parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8), listener);
    }

    @Override
    public void parse(Reader reader, TraceListener listener) {
        List<Span> spans = new ArrayList<>();

        try (JsonParser parser = factory.createParser(reader)) {

            JsonToken token;

            // JSONL contains multiple independent root objects.
            while ((token = parser.nextToken()) != null) {

                if (token != JsonToken.START_OBJECT) {
                    throw new RuntimeException("Expected JSON object");
                }

                JsonNode spanNode = mapper.readTree(parser);

                String name = spanNode.path("operationName").asText();
                String traceId = spanNode.path("traceID").asText();
                String spanId = spanNode.path("spanID").asText();

                String parentSpanId = extractParentSpanId(spanNode);

                long startTime = spanNode.path("startTime").asLong();
                long duration = spanNode.path("duration").asLong();

                String kind = null;
                String tracer = null;

                Attributes attributes = new Attributes();

                // Extract span attributes.
                JsonNode tags = spanNode.path("tag");

                if (tags.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> fields =
                            tags.fields();

                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();

                        String key = field.getKey();
                        JsonNode value = field.getValue();

                        // Normalize proprietary attribute naming.
                        String normalizedKey = key.replace('@', '.');

                        if ("span.kind".equals(normalizedKey)) {
                            kind = value.asText();
                            continue;
                        }

                        if ("otel.scope.name".equals(normalizedKey)) {
                            tracer = value.asText();
                            continue;
                        }

                        putAttribute(attributes, normalizedKey, value);
                    }
                }

                // Extract process/resource attributes.
                JsonNode process = spanNode.path("process");

                if (process.isObject()) {

                    String serviceName = process.path("serviceName").asText(null);

                    if (serviceName != null) {
                        attributes.put("service.name", serviceName);
                    }

                    JsonNode processTags = process.path("tag");

                    if (processTags.isObject()) {
                        Iterator<Map.Entry<String, JsonNode>> fields =
                                processTags.fields();

                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> field = fields.next();

                            String key = field.getKey().replace('@', '.');

                            putAttribute(attributes, key, field.getValue());
                        }
                    }
                }

                Span span = new Span(
                        name,
                        spanId,
                        traceId,
                        parentSpanId,
                        kind,
                        tracer,
                        startTime,
                        startTime + duration
                );

                span.putAttributes(attributes);
                spans.add(span);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse OpenTelemetry JSONL", e);
        }

        listener.spansCollected(spans);
    }

    private void putAttribute(
            Attributes attributes,
            String key,
            JsonNode value) {

        if (value == null || value.isNull()) {
            return;
        }

        if (value.isTextual()) {
            attributes.put(key, value.asText());
        } else if (value.isBoolean()) {
            attributes.put(key, value.asBoolean());
        } else if (value.isIntegralNumber()) {
            attributes.put(key, value.asLong());
        } else if (value.isFloatingPointNumber()) {
            attributes.put(key, value.asDouble());
        } else {
            // Preserve complex values as JSON strings.
            attributes.put(key, value.toString());
        }
    }

    private String extractParentSpanId(JsonNode spanNode) {
        JsonNode references = spanNode.path("references");

        if (references.isArray()) {
            for (JsonNode reference : references) {
                if ("CHILD_OF".equals(reference.path("refType").asText())) {
                    return reference.path("spanID").asText();
                }
            }
        }

        return null;
    }
}

