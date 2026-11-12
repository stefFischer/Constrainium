package at.scch.traceon;

import at.scch.traceon.otel.HttpPayloadDecoder;
import at.scch.traceon.otel.MongoPayloadDecoder;
import at.scch.traceon.otel.PayloadDecoder;
import at.scch.traceon.otel.RedisPayloadDecoder;
import at.scch.traceon.otel.dataextraction.HttpSpanDataExtractor;
import at.scch.traceon.otel.dataextraction.MongoSpanDataExtractor;
import at.scch.traceon.otel.dataextraction.RedisSpanDataExtractor;
import at.scch.traceon.otel.parser.OpenTelemetryJsonlParser;
import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.*;
import at.sfischer.constraints.model.NumberLiteral;
import at.sfischer.constraints.model.StringLiteral;
import at.sfischer.constraints.model.Value;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.logic.AndOperator;
import at.sfischer.constraints.model.operators.numbers.EqualOperator;
import at.sfischer.constraints.model.operators.numbers.GreaterThanOrEqualOperator;
import at.sfischer.constraints.model.operators.numbers.LessThanOrEqualOperator;
import at.sfischer.constraints.model.operators.strings.StringEquals;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.abstraction.SpanAbstractor;
import at.sfischer.traces.otel.abstraction.TraceAbstractor;
import at.sfischer.traces.otel.collector.FileCollector;
import at.sfischer.traces.otel.collector.buffer.TraceBuffer;
import at.sfischer.traces.otel.dataextraction.SpanData;
import at.sfischer.traces.otel.filter.MinSpanCountFilter;
import at.sfischer.traces.otel.matching.SpanMatch;
import at.sfischer.traces.otel.processing.TraceProcessor;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {

    private static HttpSpanDataExtractor HTTP_DATA_EXTRACTOR;

    private static MongoSpanDataExtractor MONGO_DATA_EXTRACTOR;
    private static RedisSpanDataExtractor REDIS_DATA_EXTRACTOR;

    private static final PayloadDecoder HTTP_PAYLOAD_DECODER = new HttpPayloadDecoder();
    private static final PayloadDecoder MONGO_PAYLOAD_DECODER = new MongoPayloadDecoder();
    private static final PayloadDecoder REDIS_PAYLOAD_DECODER = new RedisPayloadDecoder();


    private final static SpanAbstractor HTTP_SERVER_ABSTRACTOR = new SpanAbstractor(SpanMatch.kind("server").and(SpanMatch.attributeExists("http.request.method"))) {
        @Override
        public AbstractSpan abstractSpan(Span span) {
            String serviceName = span.getAttribute("service.name");
            if(serviceName.equals("simulation-backend")){
                return null;
            }

            AbstractSpan abstractSpan = new AbstractSpan(span.getName(), span.getKind());
            abstractSpan.getAttributes().put("tracer", span.getTracer());
            transferAttributes(span, abstractSpan,
                    "service.name",
                    "http.request.method",
                    "http.response.status_code");

            System.out.println(span);
            printSpanData("server span data", HTTP_DATA_EXTRACTOR.extractData(span));

            return abstractSpan;
        }
    };

    private final static SpanAbstractor HTTP_CLIENT_ABSTRACTOR = new SpanAbstractor(SpanMatch.kind("client").and(SpanMatch.attributeExists("http.request.method"))) {
        @Override
        public AbstractSpan abstractSpan(Span span) {
            String serviceName = span.getAttribute("service.name");
            if(serviceName.equals("simulation-backend")){
                return null;
            }

            AbstractSpan abstractSpan = new AbstractSpan(span.getName(), span.getKind());
            abstractSpan.getAttributes().put("tracer", span.getTracer());
            transferAttributes(span, abstractSpan,
                    "service.name",
                    "http.request.method",
                    "http.response.status_code");

            System.out.println(span);
            printSpanData("client span data", HTTP_DATA_EXTRACTOR.extractData(span));

            return abstractSpan;
        }
    };

    private final static SpanAbstractor MONGO_DB_ABSTRACTOR = new SpanAbstractor(SpanMatch.kind("client").and(SpanMatch.attribute("db.system", "mongodb"))) {
        @Override
        public AbstractSpan abstractSpan(Span span) {
            AbstractSpan abstractSpan = new AbstractSpan(span.getName(), "MONGO");
            abstractSpan.getAttributes().put("tracer", span.getTracer());
            transferAttributes(span, abstractSpan,
                    "service.name",
                    "db.system",
                    "db.operation.name");

            System.out.println(span);
            printSpanData("mongo data", MONGO_DATA_EXTRACTOR.extractData(span));

            return abstractSpan;
        }
    };

    private final static SpanAbstractor REDIS_ABSTRACTOR = new SpanAbstractor(SpanMatch.kind("client").and(SpanMatch.attribute("db.system", "redis"))) {
        @Override
        public AbstractSpan abstractSpan(Span span) {
            AbstractSpan abstractSpan = new AbstractSpan(span.getName(), "REDIS");
            abstractSpan.getAttributes().put("tracer", span.getTracer());
            transferAttributes(span, abstractSpan,
                    "service.name",
                    "db.system",
                    "db.name",
                    "db.operation.name",
                    "db.operation.batch.size");

            System.out.println(span);
            printSpanData("redis data", REDIS_DATA_EXTRACTOR.extractData(span));

            return abstractSpan;
        }
    };

    private static final TraceAbstractor ABSTRACTOR = new TraceAbstractor (
            HTTP_SERVER_ABSTRACTOR,
            HTTP_CLIENT_ABSTRACTOR,
            MONGO_DB_ABSTRACTOR,
            REDIS_ABSTRACTOR
    );

    public static void main(String[] args) throws IOException {
//        File traces = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/Traces/2026-09-23T161537-6.200.json");
//        File data = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/Traces/captures.jsonl");
//        JaegerParser parser = new JaegerParser();

        File traces = new File("C:/Users/sfischer/Downloads/arch-7d0f5cd6-307b-4bb8-b296-3b7010c7eeb2/spans.jsonl");
        File data = new File("C:/Users/sfischer/Downloads/arch-7d0f5cd6-307b-4bb8-b296-3b7010c7eeb2/captures.jsonl");
        OpenTelemetryJsonlParser parser = new OpenTelemetryJsonlParser();

        SimpleDataCollection dataCollection = SimpleDataCollection.parseData(data);

        FileCollector collector = new FileCollector(traces, parser);
        TraceBuffer buffer = new TraceBuffer(5);

        HTTP_DATA_EXTRACTOR = new HttpSpanDataExtractor(dataCollection, HTTP_PAYLOAD_DECODER);
        MONGO_DATA_EXTRACTOR = new MongoSpanDataExtractor(dataCollection, MONGO_PAYLOAD_DECODER);
        REDIS_DATA_EXTRACTOR = new RedisSpanDataExtractor(dataCollection, REDIS_PAYLOAD_DECODER);

        TraceProcessor<Span, AbstractSpan> pipeline = new TraceProcessor<Span, Span>() {
            @Override
            public List<Span> process(List<Span> input) {
                return input;
            }
        }
        .pipe(new MinSpanCountFilter<>(2))
        .pipe(input -> {
            for (Span span : input) {
                System.out.println(span.toStringWithChildren());
            }

            return input;
        })
        .pipe(ABSTRACTOR)
        .pipe(input -> {
            for (AbstractSpan span : input) {
                System.out.println(span.toStringWithChildren());
            }

            return input;
        });


        buffer.addTraceListener(pipeline::process);

        collector.addTraceListener(buffer);
        collector.collect();

        buffer.flush();


        System.exit(0);

        System.out.println(dataCollection.size());

        SimpleDataSchema schema = dataCollection.deriveSchema(null);

        System.out.println(schema);

        for (DataObject object : dataCollection.getDataCollection()) {

            System.out.println(object.toJson());
        }

    }

    private static void printSpanData(String label, SpanData data) {
        System.out.println(label + ":");
        if (data == null) {
            return;
        }

        DataObject input = data.getInputData();
        DataObject output = data.getOutputData();
        System.out.println("  input: " + (input == null ? "null" : input.toJson()));
        System.out.println("  output: " + (output == null ? "null" : output.toJson()));
    }
}