package at.sfischer.traces.otel.parser;

import at.sfischer.traces.otel.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.util.*;

public interface TraceParser {
    Logger LOGGER = LoggerFactory.getLogger(TraceParser.class);

    List<Span> parse(Reader reader);

    List<Span> parse(InputStream inputStream);


    static List<Span> recreateHierarchy(Collection<Span> spans){
        Map<String ,Span> traceRoots = new LinkedHashMap<>();
        Map<String ,Span> spansMap = new LinkedHashMap<>();

        for (Span span : spans) {
            spansMap.put(span.getSpanId(), span);
            if(span.getParentSpanId() == null){
                traceRoots.put(span.getTraceId(), span);
            }
        }

        return recreateHierarchy(traceRoots, spansMap);
    }

    private static List<Span> recreateHierarchy(Map<String ,Span> traceRoots, Map<String ,Span> spans){
        // Create hierarchy of traces.
        List<Span> spanList = new LinkedList<>();
        for (Map.Entry<String, Span> spanEntry : spans.entrySet()) {
            Span span = spanEntry.getValue();
            if(span.getParentSpanId() != null){
                Span parent = spans.get(span.getParentSpanId());
                if(parent != null){
                    parent.children.add(span);
                } else {
                    // Check if we have a root span with the same traceId.
                    Span root = traceRoots.get(span.getTraceId());
                    if(root != null){
                        root.children.add(span);
                    } else {
                        LOGGER.error("No parent for span: {}, parentSpanId: {}, traceId: {}", span, span.getParentSpanId(), span.getTraceId());
                    }
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
        spans.forEach(d -> sortByTime(d.children));
    }
}
