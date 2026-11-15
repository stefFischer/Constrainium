package at.scch.traceon.otel.dataextraction;

import at.scch.traceon.otel.PayloadDecoder;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.DataValue;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.TraceNode;
import at.sfischer.traces.otel.dataextraction.DataExtractor;
import at.sfischer.traces.otel.dataextraction.SpanData;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSpanDataExtractor implements DataExtractor {

    private static final String PAYLOAD_INLINE = "payload_inline";
    private static final String RESULTS = "results";
    private static final String PAYLOAD_DECODE_ERROR = "payload_decode_error";

    private final PayloadDecoder payloadDecoder;

    protected AbstractSpanDataExtractor(PayloadDecoder payloadDecoder) {
        this.payloadDecoder = payloadDecoder;
    }

    public SpanData extractData(Span span) {
        return new SpanData(
                toDataObject(findRequestData(span)),
                toDataObject(findResponseData(span))
        );
    }

    @Override
    public SpanData extractData(TraceNode<?> span) {
        if (!(span instanceof Span otelSpan)) {
            throw new IllegalArgumentException(
                    "Expected " + Span.class.getSimpleName() + " but got "
                            + (span == null ? "null" : span.getClass().getSimpleName())
            );
        }

        return extractData(otelSpan);
    }

    protected abstract DataCollection<DataObject> findRequestData(Span span);

    protected abstract DataCollection<DataObject> findResponseData(Span span);

    protected DataObject toDataObject(DataCollection<DataObject> captures) {
        List<DataObject> payloads = new ArrayList<>();
        captures.visitDataObjects((dataObject, dataEntry) -> {
            DataObject payload = decodePayload(dataObject);
            if (payload != null) {
                payloads.add(payload);
            }
        });

        if (payloads.isEmpty()) {
            return null;
        }

        if (payloads.size() == 1) {
            return payloads.get(0);
        }

        DataObject result = new DataObject();
        result.putValue(RESULTS, payloads.toArray(DataObject[]::new));
        return result;
    }

    private DataObject decodePayload(DataObject capture) {
        DataValue<?> payloadValue = capture.getDataValue(PAYLOAD_INLINE);
        if (payloadValue == null || !(payloadValue.getValue() instanceof String payload)) {
            return null;
        }

        try {
            return payloadDecoder.decode(payload);
        } catch (RuntimeException e) {
            DataObject result = new DataObject();
            result.putValue(PAYLOAD_DECODE_ERROR, e.toString());
            return result;
        }
    }
}
