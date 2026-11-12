package at.scch.traceon.otel.dataextraction;

import at.scch.traceon.otel.PayloadDecoder;
import at.scch.traceon.otel.data.MongoRequestSpanDataFinder;
import at.scch.traceon.otel.data.MongoResponseSpanDataFinder;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.traces.otel.Span;

public class MongoSpanDataExtractor extends AbstractSpanDataExtractor {

    private final MongoRequestSpanDataFinder requestFinder;
    private final MongoResponseSpanDataFinder responseFinder;

    public MongoSpanDataExtractor(DataCollection<DataObject> captures, PayloadDecoder payloadDecoder) {
        this(new MongoRequestSpanDataFinder(captures), new MongoResponseSpanDataFinder(captures), payloadDecoder);
    }

    public MongoSpanDataExtractor(
            MongoRequestSpanDataFinder requestFinder,
            MongoResponseSpanDataFinder responseFinder,
            PayloadDecoder payloadDecoder) {
        super(payloadDecoder);
        this.requestFinder = requestFinder;
        this.responseFinder = responseFinder;
    }

    @Override
    protected DataCollection<DataObject> findRequestData(Span span) {
        return requestFinder.findData(span);
    }

    @Override
    protected DataCollection<DataObject> findResponseData(Span span) {
        return responseFinder.findData(span);
    }
}
