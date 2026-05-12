package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.TraceNode;
import at.sfischer.traces.otel.collector.RecordingTraceListener;
import at.sfischer.traces.otel.matching.*;

import java.util.*;

public interface TraceAssertion {

    /**
     * Asserts that the span that matches with the SpanMatches is received within the timeout.
     *
     * @param listener RecordingTraceListener to receive spans
     * @param timeout maximum time in milliseconds for the span to arrive. If it is not received before this timeout is exceeded the assertion fails with an AssertionError.
     * @param matchers SpanMatches to check if the received span matches the expected one
     */
    static <T extends TraceNode<T>> void assertSpan(RecordingTraceListener<T> listener, long timeout, Collection<TraceNodeMatch<T>> matchers){
        //noinspection unchecked
        assertSpan(listener, timeout, matchers.toArray(new TraceNodeMatch[0]));
    }

    /**
     * Asserts that the span that matches with the SpanMatches is received within the timeout.
     * This looks for a span that all SpanMatches apply to (i.e., all matchers are combined in an AndSpanMatch).
     *
     * @param listener RecordingTraceListener to receive spans
     * @param timeout maximum time in milliseconds for the span to arrive. If it is not received before this timeout is exceeded the assertion fails with an AssertionError.
     * @param matchers SpanMatches to check if the received span matches the expected one
     */
    static <T extends TraceNode<T>> void assertSpan(RecordingTraceListener<T> listener, long timeout, TraceNodeMatch<T>... matchers){
        AndSpanMatch<T> matcher = new AndSpanMatch<>(matchers);
        Map<T, MatchResult> mismatches = new HashMap<>();
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis() < startTime + timeout){
            List<T> spans = listener.drain();
            for (T span : spans) {
                MatchResult result = matcher.matches(span);
                if(result == MatchResult.SUCCESS){
                    return;
                }

                mismatches.put(span, result);
            }
        }

        StringBuilder mismatchMessage = new StringBuilder();
        for (Map.Entry<T, MatchResult> entry : mismatches.entrySet()) {
            T key = entry.getKey();
            if(key instanceof Span){
                mismatchMessage.append("Span: ").append(((Span)key).getSpanId()).append("\n");
            } else {
                mismatchMessage.append("TraceNode: ").append(key.toString()).append("\n");
            }
            mismatchMessage.append(entry.getValue().resultMessage()).append("\n");
        }

        throw new AssertionError("Expected Span not received:\n" + mismatchMessage);
    }

    /**
     * Asserts that the span with the given data is received within the timeout.
     *
     * @param listener RecordingTraceListener to receive spans
     * @param timeout maximum time in milliseconds for the span to arrive. If it is not received before this timeout is exceeded the assertion fails with an AssertionError.
     * @param name of the expected span
     * @param kind of the expected span
     */
    static <T extends TraceNode<T>> void assertSpan(RecordingTraceListener<T> listener, long timeout, String name, String kind){
        //noinspection unchecked
        assertSpan(listener, timeout, new NameMatch<>(name), new KindMatch<>(kind));
    }

    /**
     * Asserts that the span with the given data is received within the timeout.
     *
     * @param listener RecordingTraceListener to receive spans
     * @param timeout maximum time in milliseconds for the span to arrive. If it is not received before this timeout is exceeded the assertion fails with an AssertionError.
     * @param name of the expected span
     * @param kind of the expected span
     * @param attributes which the expected span should contain as a map with the key being the attribute name and the value the attribute value
     */
    static <T extends TraceNode<T>> void assertSpan(RecordingTraceListener<T> listener, long timeout, String name, String kind, Map<String, Object> attributes){
        List<TraceNodeMatch<T>> matchers = new LinkedList<>(Arrays.asList(new NameMatch<>(name), new KindMatch<>(kind)));
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            AttributeMatch<?, T> match = new AttributeMatch<>(entry.getKey(), entry.getValue());
            matchers.add(match);
        }

        assertSpan(listener, timeout, matchers);
    }

    /**
     * Asserts that the span with the given data is received within the timeout.
     *
     * @param listener RecordingTraceListener to receive spans
     * @param timeout maximum time in milliseconds for the span to arrive. If it is not received before this timeout is exceeded the assertion fails with an AssertionError.
     * @param nameRegex regular expression that the name of the span should match
     * @param kindRegex regular expression that the kind of the span should match
     * @param attributesRegex regular expressions that the values of the span attributes should match, as a map (attribute name -> regular expression that the value should match)
     */
    static <T extends TraceNode<T>> void assertSpanMatch(RecordingTraceListener<T> listener, long timeout, String nameRegex, String kindRegex, Map<String, String> attributesRegex){
        List<TraceNodeMatch<T>> matchers = new LinkedList<>(Arrays.asList(new NameRegexMatch<>(nameRegex), new KindRegexMatch<>(kindRegex)));
        for (Map.Entry<String, String> entry : attributesRegex.entrySet()) {
            AttributeRegexMatch<T> match = new AttributeRegexMatch<>(entry.getKey(), entry.getValue());
            matchers.add(match);
        }

        assertSpan(listener, timeout, matchers);
    }

    /**
     * Asserts that spans that matches with the SpanMatches are received within the timeout.
     * This will try to find a span for each SpanMatch passed and each span will only be successfully matched once.
     *
     * @param listener RecordingTraceListener to receive spans
     * @param timeout maximum time in milliseconds for the span to arrive. If it is not received before this timeout is exceeded the assertion fails with an AssertionError.
     * @param matchers SpanMatches to check if the received spans match the expected ones
     */
    static <T extends TraceNode<T>> void assertSpans(RecordingTraceListener<T> listener, long timeout, TraceNodeMatch<T>... matchers){
        Map<TraceNodeMatch<T>, Map<T, MatchResult>> mismatches = new HashMap<>();
        List<TraceNodeMatch<T>> matchedSpans = new LinkedList<>();
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis() < startTime + timeout){
            List<T> spans = listener.drain();
            for (T span : spans) {
                if(matchedSpans.size() == matchers.length){
                    return;
                }
                for (TraceNodeMatch<T> matcher : matchers) {
                    if(mismatches.get(matcher) != null && mismatches.get(matcher).isEmpty()){
                        continue;
                    }

                    MatchResult result = matcher.matches(span);
                    if(result == MatchResult.SUCCESS){
                        mismatches.put(matcher, new HashMap<>());
                        matchedSpans.add(matcher);
                        break;
                    }

                    Map<T, MatchResult> misses = mismatches.computeIfAbsent(matcher, k -> new HashMap<>());
                    misses.put(span, result);
                }
            }
        }

        final boolean[] missedSpans = {false};
        mismatches.forEach((k, v) -> missedSpans[0] = missedSpans[0] || !v.isEmpty());
        if(!missedSpans[0]){
            return;
        }

        StringBuilder mismatchMessage = new StringBuilder();
        for (Map.Entry<TraceNodeMatch<T>, Map<T, MatchResult>> entry : mismatches.entrySet()) {
            if(entry.getValue().isEmpty()){
                continue;
            }

            mismatchMessage.append("Could not find span for: ").append(entry.getKey()).append("\n");
            for (Map.Entry<T, MatchResult> misses : entry.getValue().entrySet()) {
                T key = misses.getKey();
                if(key instanceof Span){
                    mismatchMessage.append("Span: ").append(((Span)key).getSpanId()).append("\n");
                } else {
                    mismatchMessage.append("TraceNode: ").append(key.toString()).append("\n");
                }
                mismatchMessage.append(misses.getValue().resultMessage()).append("\n");
            }
        }


        throw new AssertionError("Expected Spans not received:\n" + mismatchMessage);
    }
}
