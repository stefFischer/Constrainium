package at.sfischer.traces.otel.collector;

import at.sfischer.traces.otel.parser.TraceParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public abstract class FileCollector extends TraceCollector {

    private final static Logger LOGGER = LoggerFactory.getLogger(FileCollector.class);

    private final File file;

    private final TraceParser parser;

    public FileCollector(File file, TraceParser parser) {
        super();
        this.parser = parser;
        this.file = file;
    }

    @Override
    public void collect() {
        try {
            fireSpansCollected(this.parser.parse(new FileReader(this.file)));
        } catch (IOException e) {
            LOGGER.error("Error opening file", e);
        } catch (RuntimeException e) {
            LOGGER.error("Error while parsing trace", e);
        }
    }
}
