package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.Span;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TraceDifferTest {

	private Span span(String name) {
		return new Span(name, name + "Id", "trace1", null, null, null, 0, 1);
	}

	private Span span(String name, Span ... children) {
		Span s = span(name);
		for (Span child : children) {
			s.addChild(child);
		}
		return s;
	}

	@Test
	public void testNoDifference() {
		Span a = span("root", span("child1"), span("child2"));
		Span b = span("root", span("child1"), span("child2"));

		List<Difference<Span>> diffs = TraceDiffer.diff(a, b, new NameOnlySpanComparator<>());
		assertTrue(diffs.isEmpty(), "No differences expected");
	}

	@Test
	public void testSpanAdded() {
		Span a = span("root", span("child1"));
		Span b = span("root", span("child1"), span("child2"));

		List<Difference<Span>> diffs = TraceDiffer.diff(a, b, new NameOnlySpanComparator<>());
		assertEquals(1, diffs.size());
		assertEquals(Difference.Type.ADDED, diffs.getFirst().type());
		assertTrue(diffs.getFirst().message().contains("child2"));
	}

	@Test
	public void testSpanRemoved() {
		Span a = span("root", span("child1"), span("child2"));
		Span b = span("root", span("child1"));

		List<Difference<Span>> diffs = TraceDiffer.diff(a, b, new NameOnlySpanComparator<>());
		assertEquals(1, diffs.size());
		assertEquals(Difference.Type.REMOVED, diffs.getFirst().type());
		assertTrue(diffs.getFirst().message().contains("child2"));
	}

	@Test
	public void testSpanChanged() {
		Span a = span("root", span("child1"));
		Span b = span("root", span("childX"));

		List<Difference<Span>> diffs = TraceDiffer.diff(a, b, new NameOnlySpanComparator<>());
		assertEquals(1, diffs.size());
		assertEquals(Difference.Type.CHANGED, diffs.getFirst().type());
		assertTrue(diffs.getFirst().message().contains("child1"));
	}

	@Test
	public void testDifferentComparators() {
		Span a = new Span("op", "id1", "trace1", null, null, null, 0, 1);
		Span b = new Span("op", "id2", "trace1", null, null, null, 0, 1);

		SpanComparator<Span> strictIdComparator = (s1, s2) -> s1.getSpanId().equals(s2.getSpanId());
		SpanComparator<Span> nameComparator = new NameOnlySpanComparator<>();

		assertTrue(TraceDiffer.diff(a, b, nameComparator).isEmpty());
		assertEquals(1, TraceDiffer.diff(a, b, strictIdComparator).size());
	}
}
