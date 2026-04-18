package at.sfischer.traces.otel.parser;

import at.sfischer.traces.otel.Span;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TraceParserTest {

    @Test
    public void testRecreateSimpleHierarchy() {
        Span root = new Span("root", "1", "1", null, null, null, 1000, 4000);
        Span child1 = new Span("child-1", "2", "1", "1", null, null, 1500, 2000);
        Span child2 = new Span("child-2", "3", "1", "1", null, null, 2000, 2700);
        List<Span> spans = List.of(root, child1, child2);

        List<Span> spanTree = TraceParser.recreateHierarchy(spans);
        assertEquals(1, spanTree.size());

        Span expectedRoot = new Span("root", "1", "1", null, null, null, 1000, 4000);
        expectedRoot.children.add(new Span("child-1", "2", "1", "1", null, null, 1500, 2000));
        expectedRoot.children.add(new Span("child-2", "3", "1", "1", null, null, 2000, 2700));

        assertThat(spanTree.getFirst())
                .usingRecursiveComparison()
                .isEqualTo(expectedRoot);
    }

}
