package at.sfischer.traces.otel.utils;

import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.collector.RecordingTraceListener;
import at.sfischer.traces.otel.parser.TraceParser;

import java.util.List;

public interface Helpers {

    static void waitAndPrintTraces(RecordingTraceListener<Span> listener, long waitTimeMillis){
        try {
            Thread.sleep(waitTimeMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<Span> spans = listener.getAll();
        List<Span> spanList = TraceParser.recreateHierarchy(spans);
        spanList.forEach(s -> System.out.println(s.toStringWithChildren()));
    }
}
