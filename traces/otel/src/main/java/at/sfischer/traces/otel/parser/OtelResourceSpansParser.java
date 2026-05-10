package at.sfischer.traces.otel.parser;

import at.sfischer.traces.otel.Attributes;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.collector.TraceListener;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

public class OtelResourceSpansParser implements TraceParser {

    private final JsonFactory factory;

    public OtelResourceSpansParser() {
        this.factory = new JsonFactory();
    }

    @Override
    public void parse(InputStream inputStream, TraceListener listener) {
        parse(new InputStreamReader(inputStream), listener);
    }

    @Override
    public void parse(Reader reader, TraceListener listener) {
        try (JsonParser parser = factory.createParser(reader)) {

            while (parser.nextToken() != null) {
                if (parser.currentToken() != JsonToken.START_OBJECT) {
                    continue;
                }

                parseRootObject(parser, listener);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseRootObject(JsonParser parser, TraceListener listener) throws IOException {
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            List<Span> spans = new LinkedList<>();

            String fieldName = parser.currentName();
            parser.nextToken();

            if ("resourceSpans".equals(fieldName)) {
                parseResourceSpans(parser, spans);
                listener.spansCollected(spans);
            } else {
                parser.skipChildren();
            }
        }
    }

    private void parseResourceSpans(JsonParser parser, List<Span> spans) throws IOException {
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            parser.skipChildren();
            return;
        }

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            parseResourceSpan(parser, spans);
        }
    }

    private void parseResourceSpan(JsonParser parser, List<Span> spans) throws IOException {
        Attributes resourceAttributes = new Attributes();
        while (parser.nextToken() != JsonToken.END_OBJECT) {

            String field = parser.currentName();
            parser.nextToken();

            if ("scopeSpans".equals(field)) {
                parseScopeSpans(parser, spans, resourceAttributes);
            } else if ("resource".equals(field)) {
                extractResourceAttributes(parser, resourceAttributes);
            } else {
                parser.skipChildren();
            }
        }
    }

    private void extractResourceAttributes(JsonParser parser, Attributes attributes) throws IOException {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            parser.skipChildren();
            return;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            parser.nextToken();

            if ("attributes".equals(field)) {
                extractAttributesStreaming(parser, attributes);
            } else {
                parser.skipChildren();
            }
        }
    }

    private void parseScopeSpans(JsonParser parser, List<Span> spans, Attributes resourceAttributes) throws IOException {
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            parser.skipChildren();
            return;
        }

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            String tracer = null;
            Attributes scopeAttributes = new Attributes();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String field = parser.currentName();
                parser.nextToken();

                if ("scope".equals(field)) {
                    tracer = extractScope(parser, scopeAttributes);
                } else if ("spans".equals(field)) {
                    parseSpans(parser, tracer, spans, resourceAttributes);
                } else {
                    parser.skipChildren();
                }
            }
        }
    }

    private String extractScope(JsonParser parser, Attributes scopeAttributes) throws IOException {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            parser.skipChildren();
            return null;
        }

        String name = null;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            parser.nextToken();

            if ("name".equals(field)) {
                name = parser.getValueAsString();

            } else if ("attributes".equals(field)) {
                extractAttributesStreaming(parser, scopeAttributes);

            } else {
                parser.skipChildren();
            }
        }

        return name;
    }

    private void parseSpans(JsonParser parser, String tracer, List<Span> spans, Attributes resourceAttributes) throws IOException {
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            parser.skipChildren();
            return;
        }

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            spans.add(parseSpan(parser, tracer, resourceAttributes));
        }
    }

    private Span parseSpan(JsonParser parser, String tracer, Attributes resourceAttributes) throws IOException {

        String name = null, traceId = null, spanId = null, parentSpanId = null;
        long start = 0, end = 0;
        String kind = null;
        Attributes attributes = new Attributes();
        attributes.putAll(resourceAttributes);

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            parser.nextToken();

            switch (field) {
                case "name" -> name = parser.getValueAsString();
                case "traceId" -> traceId = parser.getValueAsString();
                case "spanId" -> spanId = parser.getValueAsString();
                case "parentSpanId" -> parentSpanId = parser.getValueAsString(null);
                case "startTimeUnixNano" -> start = Long.parseLong(parser.getValueAsString());
                case "endTimeUnixNano" -> end = Long.parseLong(parser.getValueAsString());
                case "kind" -> kind = mapKind(parser.getIntValue());
                case "attributes" -> extractAttributesStreaming(parser, attributes);
                default -> parser.skipChildren();
            }
        }

        Span span = new Span(name, spanId, traceId, parentSpanId, kind, tracer, start, end);
        span.putAttributes(attributes);
        return span;
    }

    private void extractAttributesStreaming(JsonParser parser, Attributes attributes) throws IOException {
        if (parser.currentToken() != JsonToken.START_ARRAY) {
            parser.skipChildren();
            return;
        }

        while (parser.nextToken() != JsonToken.END_ARRAY) {

            String key = null;

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String field = parser.currentName();
                parser.nextToken();

                if ("key".equals(field)) {
                    key = parser.getValueAsString();
                } else if ("value".equals(field)) {
                    extractAttributeValue(parser, key, attributes);
                } else {
                    parser.skipChildren();
                }
            }
        }
    }
    private void extractAttributeValue(JsonParser parser, String key, Attributes attributes) throws IOException {
        if(key == null){
            System.err.println("THIS IS A PROBLEM; key == null");
        }
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            parser.skipChildren();
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            parser.nextToken();

            switch (field) {
                case "stringValue" -> attributes.put(key, parser.getValueAsString());
                case "intValue" -> attributes.put(key, Long.parseLong(parser.getValueAsString()));
                case "boolValue" -> attributes.put(key, parser.getBooleanValue());

                // optional extensions if you need them:
                case "doubleValue" -> attributes.put(key, parser.getDoubleValue());

                case "arrayValue" -> {
                    // skip for now (or implement if needed)
                    parser.skipChildren();
                }

                case "kvlistValue" -> {
                    // nested attributes (rare); skip or recursively parse
                    parser.skipChildren();
                }

                default -> parser.skipChildren();
            }
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