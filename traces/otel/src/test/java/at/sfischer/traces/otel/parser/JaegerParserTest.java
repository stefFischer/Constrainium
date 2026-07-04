package at.sfischer.traces.otel.parser;

import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.collector.RecordingTraceListener;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JaegerParserTest {

    @Test
    public void testParserSimple() {
        String trace = """
                {
                  "data": [
                    {
                      "traceID": "1",
                      "spans": [
                        {
                          "traceID": "1",
                          "spanID": "1",
                          "operationName": "root",
                          "startTime": 123456789,
                          "duration": 1000,
                          "references": []
                        }
                      ]
                    }
                  ]
                }
                """.trim();
        JaegerParser parser = new JaegerParser();

        RecordingTraceListener listener = new RecordingTraceListener();
        parser.parse(new StringReader(trace), listener);
        List<Span> spans = listener.getAll();
        assertEquals(1, spans.size());

        Span expectedSpan = new Span("root", "1", "1", null, null,null, 123456789, 123456789 + 1000);
        assertThat(spans.getFirst())
                .usingRecursiveComparison()
                .isEqualTo(expectedSpan);
    }

    @Test
    public void testParserMultipleSpans() {
        String trace = """
            {
              "data": [
                {
                  "traceID": "1",
                  "spans": [
                    {
                      "traceID": "1",
                      "spanID": "1",
                      "operationName": "root",
                      "startTime": 1000,
                      "duration": 3000,
                      "references": []
                    },
                    {
                      "traceID": "1",
                      "spanID": "2",
                      "operationName": "child-1",
                      "startTime": 1500,
                      "duration": 500,
                      "references": [
                        {
                          "refType": "CHILD_OF",
                          "traceID": "1",
                          "spanID": "1"
                        }
                      ]
                    },
                    {
                      "traceID": "1",
                      "spanID": "3",
                      "operationName": "child-2",
                      "startTime": 2000,
                      "duration": 700,
                      "references": [
                        {
                          "refType": "CHILD_OF",
                          "traceID": "1",
                          "spanID": "1"
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """.trim();

        JaegerParser parser = new JaegerParser();
        RecordingTraceListener listener = new RecordingTraceListener();
        parser.parse(new StringReader(trace), listener);
        List<Span> spans = listener.getAll();

        assertEquals(3, spans.size());

        Span root = new Span("root", "1", "1", null, null, null, 1000, 4000);
        Span child1 = new Span("child-1", "2", "1", "1", null, null, 1500, 2000);
        Span child2 = new Span("child-2", "3", "1", "1", null, null, 2000, 2700);

        assertThat(spans)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(root, child1, child2);
    }
}
