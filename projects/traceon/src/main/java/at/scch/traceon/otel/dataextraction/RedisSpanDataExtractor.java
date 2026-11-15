package at.scch.traceon.otel.dataextraction;

import at.scch.traceon.otel.PayloadDecoder;
import at.scch.traceon.otel.data.RedisResponseSpanDataFinder;
import at.scch.traceon.otel.data.RedisSpanDataFinder;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.dataextraction.SpanData;

public class RedisSpanDataExtractor extends AbstractSpanDataExtractor {

    private final RedisSpanDataFinder requestFinder;
    private final RedisResponseSpanDataFinder responseFinder;
    private final RedisCommandDataExtractor commands;

    public RedisSpanDataExtractor(DataCollection<DataObject> captures, PayloadDecoder payloadDecoder) {
        this(new RedisSpanDataFinder(captures), new RedisResponseSpanDataFinder(captures), payloadDecoder);
    }

    public RedisSpanDataExtractor(
            RedisSpanDataFinder requestFinder,
            RedisResponseSpanDataFinder responseFinder,
            PayloadDecoder payloadDecoder) {
        super(payloadDecoder);
        this.requestFinder = requestFinder;
        this.responseFinder = responseFinder;
        this.commands = new RedisCommandDataExtractor(payloadDecoder);
    }

    @Override
    public SpanData extractData(Span span) {
        DataCollection<DataObject> requests = findRequestData(span);
        DataCollection<DataObject> responses = findResponseData(span);
        return commands.enrich(requests, responses, toDataObject(requests), toDataObject(responses));
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
