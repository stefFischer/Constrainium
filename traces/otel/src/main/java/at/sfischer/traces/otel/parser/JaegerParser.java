package at.sfischer.traces.otel.parser;

import at.sfischer.traces.otel.Attributes;
import at.sfischer.traces.otel.Span;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

public class JaegerParser implements TraceParser {

    private final JsonFactory factory;

    private final ObjectMapper mapper;

    public JaegerParser() {
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

            // Ensure we're at the root object
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new RuntimeException("Expected JSON root object");
            }

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = parser.currentName();

                if ("data".equals(fieldName)) {
                    parser.nextToken(); // move to START_ARRAY
                    if (parser.currentToken() != JsonToken.START_ARRAY) {
                        throw new RuntimeException("Expected 'data' to be an array");
                    }

                    // Process each element in the 'data' array
                    while (parser.nextToken() == JsonToken.START_OBJECT) {
                        JsonNode dataNode = mapper.readTree(parser);

                        JsonNode spanNodes = dataNode.get("spans");
                        if (spanNodes != null && spanNodes.isArray()) {
                            for (JsonNode spanNode : spanNodes) {
                                String name = spanNode.path("operationName").asText();
                                String traceId = spanNode.path("traceID").asText();
                                String spanId = spanNode.path("spanID").asText();
                                String parentSpanId = extractParentSpanId(spanNode);

                                long startTime = spanNode.path("startTime").asLong();
                                long duration = spanNode.path("duration").asLong();

                                String kind = null;
                                String tracer = null;
                                Attributes attributes = new Attributes();

                                JsonNode tags = spanNode.get("tags");
                                if (tags != null && tags.isArray()) {
                                    for (JsonNode tagNode : tags) {
                                        String key = tagNode.path("key").asText();
                                        String type = tagNode.path("type").asText();

                                        if ("span.kind".equals(key)) {
                                            kind = tagNode.path("value").asText();
                                            continue;
                                        }
                                        if ("otel.scope.name".equals(key)) {
                                            tracer = tagNode.path("value").asText();
                                            continue;
                                        }

                                        switch (type) {
                                            case "string":
                                                attributes.put(key, tagNode.path("value").asText());
                                                break;
                                            case "int64":
                                                attributes.put(key, tagNode.path("value").asLong());
                                                break;
                                            case "bool":
                                                attributes.put(key, tagNode.path("value").asBoolean());
                                                break;
                                            default:
                                                throw new IllegalStateException("Unsupported type: " + type);
                                        }
                                    }
                                }

                                Span span = new Span(name, spanId, traceId, parentSpanId, kind, tracer, startTime, startTime + duration);
                                span.putAttributes(attributes);
                                spans.add(span);
                            }
                        }
                    }
                } else {
                    parser.skipChildren(); // skip fields other than 'data'
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return spans;
    }

    private String extractParentSpanId(JsonNode spanNode) {
        JsonNode refs = spanNode.get("references");
        if (refs != null && refs.isArray()) {
            for (JsonNode ref : refs) {
                if ("CHILD_OF".equals(ref.path("refType").asText())) {
                    return ref.path("spanID").asText();
                }
            }
        }
        return null;
    }
}
