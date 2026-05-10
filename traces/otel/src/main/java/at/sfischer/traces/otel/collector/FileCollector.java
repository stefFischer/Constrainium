package at.sfischer.traces.otel.collector;

import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.parser.TraceParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class FileCollector extends TraceCollector {

    private final static Logger LOGGER = LoggerFactory.getLogger(FileCollector.class);

    private final File file;

    private final TraceParser parser;

    private final TraceListener<Span> internalListener = new TraceListener<>() {
        @Override
        public void spansCollected(List<Span> spans) {
            fireSpansCollected(spans);
        }
    };

    public FileCollector(File file, TraceParser parser) {
        super();
        this.parser = parser;
        this.file = file;
    }

    @Override
    public void collect() {
        try {
            this.parser.parse(new FileReader(this.file), internalListener);
        } catch (IOException e) {
            LOGGER.error("Error opening file", e);
            e.printStackTrace();
        } catch (RuntimeException e) {
            LOGGER.error("Error while parsing trace", e);
            e.printStackTrace();
        }
    }
}
