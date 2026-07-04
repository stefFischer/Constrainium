package at.sfischer.traces.otel.collector.grpc;

import at.sfischer.traces.otel.collector.TraceCollector;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OtelGrpcCollector extends TraceCollector {

    public final static int DEFAULT_GRPC_PORT = 4317;

    private final static Logger LOGGER = LoggerFactory.getLogger(OtelGrpcCollector.class);

    private final Server server;

    public OtelGrpcCollector(int port) {
        this.server = ServerBuilder
                .forPort(port)
                .addService(new OtelGrpcReceiver(this::fireSpansCollected))
                .build();
    }

    public void start() throws IOException {
        server.start();
    }

    public void stop() {
        server.shutdown();
    }

    @Override
    public void collect() {
        try {
            start();
        } catch (IOException e) {
            LOGGER.error("Error opening file", e);
        }
    }
}