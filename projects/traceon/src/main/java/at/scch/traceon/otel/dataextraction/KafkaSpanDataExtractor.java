package at.scch.traceon.otel.dataextraction;

import at.scch.traceon.otel.data.KafkaSpanDataFinder;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.traces.otel.TraceNode;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.dataextraction.DataExtractor;
import at.sfischer.traces.otel.dataextraction.SpanData;

import java.util.ArrayList;
import java.util.List;

/** Matches Kafka records to the span in whose context they were published. */
public class KafkaSpanDataExtractor implements DataExtractor {
    private final KafkaSpanDataFinder dataFinder;

    public KafkaSpanDataExtractor(DataCollection<DataObject> records) {
        this(new KafkaSpanDataFinder(records));
    }

    public KafkaSpanDataExtractor(KafkaSpanDataFinder dataFinder) {
        this.dataFinder = dataFinder;
    }

    @Override
    public SpanData extractData(TraceNode<?> node) {
        if (!(node instanceof Span span)) {
            throw new IllegalArgumentException("Expected an OpenTelemetry Span");
        }
        List<DataObject> payloads = new ArrayList<>();
        dataFinder.findData(span).visitDataObjects((record, entry) -> {
            var payload = record.getValues("payload");
            if (payload == null || payload.isEmpty()) return;
            DataObject result = new DataObject();
            result.putNodeValue("payload", payload.getFirst());
            payloads.add(result);
        });
        DataObject output = null;
        if (payloads.size() == 1) {
            output = payloads.getFirst();
        } else if (!payloads.isEmpty()) {
            output = new DataObject();
            output.putValue("results", payloads.toArray(DataObject[]::new));
        }
        // The IDs describe publication, not consumption, regardless of span kind.
        return new SpanData(null, output);
    }

}
