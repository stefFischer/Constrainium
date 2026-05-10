package at.sfischer.traces.otel.collector;

import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.parser.TraceParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.Reader;
import java.util.List;

import static org.mockito.Mockito.*;

public class FileCollectorTest {

    @Test
    public void testCollectCallsListenerWithParsedSpans() throws Exception {
        File tempFile = File.createTempFile("trace", ".json");

        List<Span> parsedSpans = List.of(
                new Span("test", "1", "1", null, null, null, 0, 1)
        );

        TraceParser parser = mock(TraceParser.class);

        doAnswer(invocation -> {
            TraceListener listenerArg = invocation.getArgument(1);
            listenerArg.spansCollected(parsedSpans);
            return null;
        }).when(parser).parse(any(Reader.class), any(TraceListener.class));

        FileCollector collector = new FileCollector(tempFile, parser);

        TraceListener listener = mock(TraceListener.class);
        collector.addTraceListener(listener);

        collector.collect();

        verify(parser).parse(any(Reader.class), any(TraceListener.class));
        verify(listener).spansCollected(parsedSpans);
    }

    @Test
    public void testCollectNotifiesAllListeners() throws Exception {
        File tempFile = File.createTempFile("trace", ".json");

        List<Span> parsedSpans = List.of(
                new Span("test", "1", "1", null, null, null, 0, 1)
        );

        TraceParser parser = mock(TraceParser.class);

        doAnswer(invocation -> {
            TraceListener listenerArg = invocation.getArgument(1);
            listenerArg.spansCollected(parsedSpans);
            return null;
        }).when(parser).parse(any(Reader.class), any(TraceListener.class));

        FileCollector collector = new FileCollector(tempFile, parser);

        TraceListener listener1 = mock(TraceListener.class);
        TraceListener listener2 = mock(TraceListener.class);

        collector.addTraceListener(listener1);
        collector.addTraceListener(listener2);

        collector.collect();

        verify(listener1).spansCollected(parsedSpans);
        verify(listener2).spansCollected(parsedSpans);
    }

    @Test
    public void testRemoveListener() throws Exception {
        File tempFile = File.createTempFile("trace", ".json");

        List<Span> parsedSpans = List.of(
                new Span("test", "1", "1", null, null, null, 0, 1)
        );

        TraceParser parser = mock(TraceParser.class);

        doAnswer(invocation -> {
            TraceListener listenerArg = invocation.getArgument(1);
            listenerArg.spansCollected(parsedSpans);
            return null;
        }).when(parser).parse(any(Reader.class), any(TraceListener.class));

        FileCollector collector = new FileCollector(tempFile, parser);

        TraceListener listener = mock(TraceListener.class);

        collector.addTraceListener(listener);
        collector.removeTraceListener(listener);

        collector.collect();

        verify(listener, never()).spansCollected(any());
    }

    @Test
    public void testCollectHandlesParserException() throws Exception {
        File tempFile = File.createTempFile("trace", ".json");

        TraceParser parser = mock(TraceParser.class);

        doThrow(new RuntimeException("fail"))
                .when(parser)
                .parse(any(Reader.class), any(TraceListener.class));

        FileCollector collector = new FileCollector(tempFile, parser);

        TraceListener listener = mock(TraceListener.class);
        collector.addTraceListener(listener);

        collector.collect();

        verify(listener, never()).spansCollected(any());
    }
}
