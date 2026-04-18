package at.sfischer.traces.otel.parser;

import at.sfischer.traces.otel.Span;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OtelResourceSpansParserTest {

    @Test
    public void testParserSimple() {
        String trace = """
            {
              "resourceSpans": [
                {
                  "scopeSpans": [
                    {
                      "scope": {
                        "name": "test-scope"
                      },
                      "spans": [
                        {
                          "traceId": "1",
                          "spanId": "1",
                          "name": "root",
                          "startTimeUnixNano": 123456789,
                          "endTimeUnixNano": 123457789
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """.trim();

        OtelResourceSpansParser parser = new OtelResourceSpansParser();

        List<Span> spans = parser.parse(new StringReader(trace));
        assertEquals(1, spans.size());

        Span expectedSpan = new Span(
                "root",
                "1",   // spanId
                "1",   // traceId
                null,  // parent
                null,  // kind
                "test-scope", // tracer
                123456789,
                123457789
        );

        assertThat(spans.getFirst())
                .usingRecursiveComparison()
                .isEqualTo(expectedSpan);
    }

    @Test
    public void testParserMultipleSpans() {
        String trace = """
        {
          "resourceSpans": [
            {
              "scopeSpans": [
                {
                  "scope": {
                    "name": "test-scope"
                  },
                  "spans": [
                    {
                      "traceId": "1",
                      "spanId": "1",
                      "name": "root",
                      "startTimeUnixNano": 1000,
                      "endTimeUnixNano": 4000
                    },
                    {
                      "traceId": "1",
                      "spanId": "2",
                      "parentSpanId": "1",
                      "name": "child-1",
                      "startTimeUnixNano": 1500,
                      "endTimeUnixNano": 2000
                    },
                    {
                      "traceId": "1",
                      "spanId": "3",
                      "parentSpanId": "1",
                      "name": "child-2",
                      "startTimeUnixNano": 2000,
                      "endTimeUnixNano": 2700
                    }
                  ]
                }
              ]
            }
          ]
        }
        """.trim();

        OtelResourceSpansParser parser = new OtelResourceSpansParser();
        List<Span> spans = parser.parse(new StringReader(trace));

        assertEquals(3, spans.size());

        Span root = new Span("root", "1", "1", null, null, "test-scope", 1000, 4000);
        Span child1 = new Span("child-1", "2", "1", "1", null, "test-scope", 1500, 2000);
        Span child2 = new Span("child-2", "3", "1", "1", null, "test-scope", 2000, 2700);

        assertThat(spans)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(root, child1, child2);
    }
}
