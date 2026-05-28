package at.sfischer.traces.otel.dataextraction;

import at.sfischer.traces.otel.TraceNode;

public interface DataExtractor {

    SpanData extractData(TraceNode<?> span);
}
