package at.scch.traceon.otel.dataextraction;

import at.scch.traceon.otel.PayloadDecoder;
import at.scch.traceon.otel.data.HttpResponseSpanDataFinder;
import at.scch.traceon.otel.data.HttpSpanDataFinder;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.dataextraction.SpanData;

public class HttpSpanDataExtractor extends AbstractSpanDataExtractor {

    private final HttpSpanDataFinder requestFinder;
    private final HttpResponseSpanDataFinder responseFinder;
    private final HttpUrlParameterExtractor urlParameterExtractor = new HttpUrlParameterExtractor();

    public HttpSpanDataExtractor(DataCollection<DataObject> captures, PayloadDecoder payloadDecoder) {
        this(new HttpSpanDataFinder(captures), new HttpResponseSpanDataFinder(captures), payloadDecoder);
    }

    public HttpSpanDataExtractor(
            HttpSpanDataFinder requestFinder,
            HttpResponseSpanDataFinder responseFinder,
            PayloadDecoder payloadDecoder) {
        super(payloadDecoder);
        this.requestFinder = requestFinder;
        this.responseFinder = responseFinder;
    }

    @Override
    public SpanData extractData(Span span) {
        DataObject input = toDataObject(findRequestData(span));
        DataObject urlParams = urlParameterExtractor.extract(span);

        if (urlParams != null) {
            if (input == null) {
                input = urlParams;
            } else {
                input = input.clone();
                input.putDataValues(urlParams);
            }
        }

        return new SpanData(input, toDataObject(findResponseData(span)));
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
