package at.scch.traceon.otel.dataextraction;

import at.scch.traceon.otel.data.KafkaSpanDataFinder;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.model.Value;
import at.sfischer.traces.otel.TraceNode;
import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.dataextraction.DataExtractor;
import at.sfischer.traces.otel.dataextraction.SpanData;

import java.util.ArrayList;
import java.util.List;

/** Extracts the Kafka record payloads matched by the data finder. */
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
            List<Value<?>> payload = record.getValues("payload");
            if (payload == null || payload.isEmpty()){
                return;
            }

            DataObject result = new DataObject();
            result.putNodeValue("payload", payload.getFirst());
            payloads.add(result);
        });

        DataObject data = null;
        if (payloads.size() == 1) {
            data = payloads.getFirst();
        } else if (!payloads.isEmpty()) {
            data = new DataObject();
            data.putValue("results", payloads.toArray(DataObject[]::new));
        }
        // Preserve the input-data convention used for Kafka call edges.
        return new SpanData(data, null);
    }

}
