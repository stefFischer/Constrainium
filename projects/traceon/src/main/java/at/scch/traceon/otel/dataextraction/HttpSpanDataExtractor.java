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
        DataCollection<DataObject> requests = findRequestData(span);
        DataObject input = toDataObject(requests);
        DataObject urlParams = urlParameterExtractor.extract(span);
        DataObject capturedParams = new DataObject();
        requests.visitDataObjects((capture, entry) -> {
            DataObject parameters = urlParameterExtractor.extract(span, capture);
            if (parameters != null) {
                capturedParams.putDataValues(parameters);
            }
        });

        if (!capturedParams.getFieldNames().isEmpty()) {
            if (urlParams == null) {
                urlParams = capturedParams;
            } else {
                urlParams.putDataValues(capturedParams);
            }
        }

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
