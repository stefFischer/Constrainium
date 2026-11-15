package at.scch.traceon;

import at.scch.traceon.otel.HttpPayloadDecoder;
import at.scch.traceon.otel.MongoPayloadDecoder;
import at.scch.traceon.otel.PayloadDecoder;
import at.scch.traceon.otel.RedisPayloadDecoder;
import at.scch.traceon.otel.dataextraction.HttpSpanDataExtractor;
import at.scch.traceon.otel.dataextraction.HttpUrlParameterExtractor;
import at.scch.traceon.otel.dataextraction.KafkaSpanDataExtractor;
import at.scch.traceon.otel.dataextraction.MongoSpanDataExtractor;
import at.scch.traceon.otel.dataextraction.RedisSpanDataExtractor;
import at.scch.traceon.otel.graph.TraceonTypePromotionPolicy;
import at.scch.traceon.otel.parser.OpenTelemetryJsonlParser;
import at.scch.traceon.otel.graph.TraceonSpanFragmentBuilders;
import at.scch.traceon.otel.graph.GraphJsonFiles;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.SimpleDataCollection;
import at.sfischer.constraints.data.Utils;
import at.sfischer.constraints.data.model.Graph;
import at.sfischer.constraints.data.model.utils.GraphJsonExporter;
import at.sfischer.constraints.data.model.utils.GraphVizExporter;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.abstraction.AbstractSpan;
import at.sfischer.traces.otel.abstraction.SpanAbstractor;
import at.sfischer.traces.otel.abstraction.TraceAbstractor;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpan;
import at.sfischer.traces.otel.clustering.ClusteredAbstractSpanAccumulator;
import at.sfischer.traces.otel.clustering.TraceCluster;
import at.sfischer.traces.otel.clustering.TraceClusterer;
import at.sfischer.traces.otel.collector.FileCollector;
import at.sfischer.traces.otel.collector.buffer.TraceBuffer;
import at.sfischer.traces.otel.dataextraction.SpanData;
import at.sfischer.traces.otel.differ.AndSpanComparator;
import at.sfischer.traces.otel.differ.AttributesSpanComparator;
import at.sfischer.traces.otel.differ.NameAndKindSpanComparator;
import at.sfischer.traces.otel.differ.SpanComparator;
import at.sfischer.traces.otel.filter.MinSpanCountFilter;
import at.sfischer.traces.otel.graph.CallGraphBuilder;
import at.sfischer.traces.otel.graph.DispatchingSpanFragmentBuilder;
import at.sfischer.traces.otel.matching.SpanMatch;
import at.sfischer.traces.otel.processing.TraceProcessor;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Main {

    private static HttpSpanDataExtractor HTTP_DATA_EXTRACTOR;

    private static MongoSpanDataExtractor MONGO_DATA_EXTRACTOR;
    private static RedisSpanDataExtractor REDIS_DATA_EXTRACTOR;
    private static KafkaSpanDataExtractor KAFKA_DATA_EXTRACTOR;

    private static final PayloadDecoder HTTP_PAYLOAD_DECODER = new HttpPayloadDecoder();
    private static final PayloadDecoder MONGO_PAYLOAD_DECODER = new MongoPayloadDecoder();
    private static final PayloadDecoder REDIS_PAYLOAD_DECODER = new RedisPayloadDecoder();


    private final static SpanAbstractor HTTP_SERVER_ABSTRACTOR = new SpanAbstractor(SpanMatch.kind("server").and(SpanMatch.attributeExists("http.request.method"))) {
        @Override
        public AbstractSpan abstractSpan(Span span) {
            String serviceName = span.getAttribute("service.name");
            if(serviceName.equals("simulation-backend") || serviceName.equals("admin-backend")){
                return null;
            }

            AbstractSpan abstractSpan = createHttpAbstractSpan(span);
            abstractSpan.getAttributes().put("tracer", span.getTracer());
            transferAttributes(span, abstractSpan,
                    "service.name",
                    "http.request.method",
                    "server.address",
                    "server.port",
                    "http.response.status_code");

            SpanData data = HTTP_DATA_EXTRACTOR.extractData(span);
            abstractSpan.setSpanData(data);

            System.out.println(span);
            printSpanData("server span data", data);

            return abstractSpan;
        }
    };

    private final static SpanAbstractor HTTP_CLIENT_ABSTRACTOR = new SpanAbstractor(SpanMatch.kind("client").and(SpanMatch.attributeExists("http.request.method"))) {
        @Override
        public AbstractSpan abstractSpan(Span span) {
            String serviceName = span.getAttribute("service.name");
            if(serviceName.equals("simulation-backend") || serviceName.equals("admin-backend")){
                return null;
            }

            AbstractSpan abstractSpan = createHttpAbstractSpan(span);
            abstractSpan.getAttributes().put("tracer", span.getTracer());
            transferAttributes(span, abstractSpan,
                    "service.name",
                    "http.request.method",
                    "server.address",
                    "server.port",
                    "http.response.status_code");

            SpanData data = HTTP_DATA_EXTRACTOR.extractData(span);
            abstractSpan.setSpanData(data);

            System.out.println(span);
            printSpanData("client span data", data);

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
                    "db.name",
                    "server.address",
                    "server.port",
                    "db.operation.name");

            SpanData data = MONGO_DATA_EXTRACTOR.extractData(span);
            abstractSpan.setSpanData(data);

            System.out.println(span);
            printSpanData("mongo data", data);

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
                    "server.address",
                    "server.port",
                    "db.operation.name",
                    "db.operation.batch.size");

            SpanData data = REDIS_DATA_EXTRACTOR.extractData(span);
            abstractSpan.setSpanData(data);

            System.out.println(span);
            printSpanData("redis data", data);

            return abstractSpan;
        }
    };

    private final static SpanAbstractor KAFKA_ABSTRACTOR = new SpanAbstractor(SpanMatch.attribute("messaging.system", "kafka")) {
        @Override
        public AbstractSpan abstractSpan(Span span) {
            AbstractSpan abstractSpan = new AbstractSpan(span.getName(), span.getKind());
            abstractSpan.getAttributes().put("tracer", span.getTracer());
            transferAttributes(span, abstractSpan,
                    "service.name",
                    "messaging.system",
                    "messaging.destination.name",
                    "messaging.destination",
                    "messaging.operation",
                    "messaging.kafka.offset",
                    "messaging.kafka.partition",
                    "messaging.kafka.message.offset",
                    "messaging.kafka.destination.partition");

            SpanData data = KAFKA_DATA_EXTRACTOR.extractData(span);
            abstractSpan.setSpanData(data);

            System.out.println(span);
            printSpanData("kafka data", data);

            return abstractSpan;
        }
    };

    private static final TraceAbstractor ABSTRACTOR = new TraceAbstractor (
            HTTP_SERVER_ABSTRACTOR,
            HTTP_CLIENT_ABSTRACTOR,
            MONGO_DB_ABSTRACTOR,
            REDIS_ABSTRACTOR,
            KAFKA_ABSTRACTOR
    );

    private static TraceClusterer<AbstractSpan, ClusteredAbstractSpan> getAbstractSpanInOutputDataCollectionTraceClusterer() {
        SpanComparator<AbstractSpan> clusterComparator = new AndSpanComparator<>(
                new NameAndKindSpanComparator<AbstractSpan>(),
                new AttributesSpanComparator<>()
        );
        return new TraceClusterer<>(clusterComparator, new ClusteredAbstractSpanAccumulator());
    }

    static CallGraphBuilder CALL_GRAPH_BUILDER = new CallGraphBuilder(
            new DispatchingSpanFragmentBuilder(
                    new TraceonSpanFragmentBuilders.HttpServer(),
                    new TraceonSpanFragmentBuilders.HttpClient(),
                    new TraceonSpanFragmentBuilders.Database("MONGO"),
                    new TraceonSpanFragmentBuilders.Database("REDIS"),
                    new TraceonSpanFragmentBuilders.Kafka()
            )
    );

    public static void main(String[] args) throws IOException {
        File traces = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/hp-1-single-item-purchase-1-smoke/spans.jsonl");
        File captures = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/hp-1-single-item-purchase-1-smoke/captures.jsonl");
        File records = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/hp-1-single-item-purchase-1-smoke/records.jsonl");

//        File traces = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/hp-2-multi-item-cart-1-smoke/spans.jsonl");
//        File captures = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/hp-2-multi-item-cart-1-smoke/captures.jsonl");
//        File records = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/hp-2-multi-item-cart-1-smoke/records.jsonl");

//        File traces = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/hp-3-search-driven-1-smoke/spans.jsonl");
//        File captures = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/hp-3-search-driven-1-smoke/captures.jsonl");
//        File records = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/hp-3-search-driven-1-smoke/records.jsonl");

//        File traces = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/hp-4-reorder-1-smoke/spans.jsonl");
//        File captures = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/hp-4-reorder-1-smoke/captures.jsonl");
//        File records = new File("C:/Users/Stefan Fischer/IdeaProjects/Data/hp-4-reorder-1-smoke/records.jsonl");

//        File traces = new File("C:/Users/sfischer/Desktop/Traceon/Code/Data/hp-1-single-item-purchase-1-smoke/spans.jsonl");
//        File captures = new File("C:/Users/sfischer/Desktop/Traceon/Code/Data/hp-1-single-item-purchase-1-smoke/captures.jsonl");
//        File records = new File("C:/Users/sfischer/Desktop/Traceon/Code/Data/hp-1-single-item-purchase-1-smoke/records.jsonl");

        OpenTelemetryJsonlParser parser = new OpenTelemetryJsonlParser();

        SimpleDataCollection capturesDataCollection = SimpleDataCollection.parseData(captures);
        SimpleDataCollection recordsDataCollection = SimpleDataCollection.parseData(records);

        FileCollector collector = new FileCollector(traces, parser);
        TraceBuffer buffer = new TraceBuffer(5);

        HTTP_DATA_EXTRACTOR = new HttpSpanDataExtractor(capturesDataCollection, HTTP_PAYLOAD_DECODER);
        MONGO_DATA_EXTRACTOR = new MongoSpanDataExtractor(capturesDataCollection, MONGO_PAYLOAD_DECODER);
        REDIS_DATA_EXTRACTOR = new RedisSpanDataExtractor(capturesDataCollection, REDIS_PAYLOAD_DECODER);
        KAFKA_DATA_EXTRACTOR = new KafkaSpanDataExtractor(recordsDataCollection);

        TraceClusterer<AbstractSpan, ClusteredAbstractSpan> clusterer = getAbstractSpanInOutputDataCollectionTraceClusterer();

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
            // Remove synthetic roots.
            List<AbstractSpan> output = new LinkedList<>();
            for (AbstractSpan span : input) {
                if(span.isSyntheticRoot()){
                    output.addAll(span.getChildren());
                } else {
                    output.add(span);
                }
            }

            return output;
        })
        .pipe(input -> {
            for (AbstractSpan span : input) {
                if(span.getName().equals("POST /admin/products/reset")){
                    continue;
                }

                clusterer.add(span);
            }

            return input;
        });


        buffer.addTraceListener(pipeline::process);

        collector.addTraceListener(buffer);
        collector.collect();

        buffer.flush();

        for (TraceCluster<AbstractSpan, ClusteredAbstractSpan> cluster : clusterer.getClusters()) {
            System.out.println(cluster.getState().toStringWithChildren());
            System.out.println(cluster.getState().getSpanData().size());

            Graph g = CALL_GRAPH_BUILDER.build(cluster.getState(), new TraceonTypePromotionPolicy());

            String dot = GraphVizExporter.export(g);
            System.out.println(dot);

            GraphJsonFiles.writeUnique(g, traces.getParentFile().toPath(), cluster.getState().getName());
        }
    }

    static AbstractSpan createHttpAbstractSpan(Span span) {
        String route = new HttpUrlParameterExtractor().inferRoute(span);
        String method = span.getAttribute("http.request.method");
        String name = route != null && method != null ? method + " " + route : span.getName();
        AbstractSpan abstractSpan = new AbstractSpan(name, span.getKind());
        if (route != null){
            abstractSpan.getAttributes().put("http.route", route);
        }

        return abstractSpan;
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
