package at.sfischer.traces.otel.parser;

import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.collector.TraceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.util.*;

public interface TraceParser {
    Logger LOGGER = LoggerFactory.getLogger(TraceParser.class);

    void parse(Reader reader, TraceListener listener);

    void parse(InputStream inputStream, TraceListener listener);


    static List<Span> recreateHierarchy(Collection<Span> spans){
        Map<String ,Span> orphans = new LinkedHashMap<>();
        List<Span> spanList = recreateHierarchy(spans, orphans);
        if (LOGGER.isErrorEnabled()) {
            for (Span span : orphans.values()) {
                LOGGER.error("No parent for span: {}, parentSpanId: {}, traceId: {}", span, span.getParentSpanId(), span.getTraceId());
            }
        }

        return spanList;
    }

    static List<Span> recreateHierarchy(Collection<Span> spans, Map<String ,Span> orphans){
        Map<String ,Span> spansMap = new LinkedHashMap<>();
        for (Span span : spans) {
            spansMap.put(span.getSpanId(), span);
        }

        return recreateHierarchy(spansMap, orphans);
    }

    private static List<Span> recreateHierarchy(Map<String ,Span> spans, Map<String ,Span> orphans){
        // Create hierarchy of traces.
        List<Span> spanList = new LinkedList<>();
        for (Map.Entry<String, Span> spanEntry : spans.entrySet()) {
            Span span = spanEntry.getValue();
            if(span.getParentSpanId() != null){
                Span parent = spans.get(span.getParentSpanId());
                if(parent != null){
                    parent.addChild(span);
                } else {
                    orphans.put(span.getSpanId(), span);
                }
            } else {
                spanList.add(span);
            }
        }

        // Sort the roots by their timestamp.
        sortByTime(spanList);
        return spanList;
    }

    static void sortByTime(List<? extends Span> spans){
        spans.sort(Comparator.comparing(Span::getStart));
        spans.forEach(Span::sortChildrenByTime);
    }
}
