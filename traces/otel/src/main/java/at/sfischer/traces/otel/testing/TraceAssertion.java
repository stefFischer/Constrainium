package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.collector.RecordingTraceListener;

import java.util.*;

public interface TraceAssertion {

    /**
     * Asserts that the span that matches with the SpanMatches is received within the timeout.
     *
     * @param listener RecordingTraceListener to receive spans
     * @param timeout maximum time in milliseconds for the span to arrive. If it is not received before this timeout is exceeded the assertion fails with an AssertionError.
     * @param matchers SpanMatches to check if the received span matches the expected one
     */
    static void assertSpan(RecordingTraceListener listener, long timeout, Collection<SpanMatch> matchers){
        assertSpan(listener, timeout, matchers.toArray(new SpanMatch[0]));
    }

    /**
     * Asserts that the span that matches with the SpanMatches is received within the timeout.
     * This looks for a span that all SpanMatches apply to (i.e., all matchers are combined in an AndSpanMatch).
     *
     * @param listener RecordingTraceListener to receive spans
     * @param timeout maximum time in milliseconds for the span to arrive. If it is not received before this timeout is exceeded the assertion fails with an AssertionError.
     * @param matchers SpanMatches to check if the received span matches the expected one
     */
    static void assertSpan(RecordingTraceListener listener, long timeout, SpanMatch... matchers){
        AndSpanMatch matcher = new AndSpanMatch(matchers);
        Map<Span, MatchResult> mismatches = new HashMap<>();
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis() < startTime + timeout){
            List<Span> spans = listener.drain();
            for (Span span : spans) {
                MatchResult result = matcher.matches(span);
                if(result == MatchResult.SUCCESS){
                    return;
                }

                mismatches.put(span, result);
            }
        }

        StringBuilder mismatchMessage = new StringBuilder();
        for (Map.Entry<Span, MatchResult> entry : mismatches.entrySet()) {
            mismatchMessage.append("Span: ").append(entry.getKey().getSpanId()).append("\n");
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
    static void assertSpan(RecordingTraceListener listener, long timeout, String name, String kind){
        assertSpan(listener, timeout, new NameMatch(name), new KindMatch(kind));
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
    static void assertSpan(RecordingTraceListener listener, long timeout, String name, String kind, Map<String, Object> attributes){
        List<SpanMatch> matchers = new LinkedList<>(Arrays.asList(new NameMatch(name), new KindMatch(kind)));
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            AttributeMatch<?> match = new AttributeMatch<>(entry.getKey(), entry.getValue());
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
    static void assertSpanMatch(RecordingTraceListener listener, long timeout, String nameRegex, String kindRegex, Map<String, String> attributesRegex){
        List<SpanMatch> matchers = new LinkedList<>(Arrays.asList(new NameRegexMatch(nameRegex), new KindRegexMatch(kindRegex)));
        for (Map.Entry<String, String> entry : attributesRegex.entrySet()) {
            AttributeRegexMatch match = new AttributeRegexMatch(entry.getKey(), entry.getValue());
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
    static void assertSpans(RecordingTraceListener listener, long timeout, SpanMatch... matchers){
        Map<SpanMatch, Map<Span, MatchResult>> mismatches = new HashMap<>();
        List<SpanMatch> matchedSpans = new LinkedList<>();
        long startTime = System.currentTimeMillis();
        while(System.currentTimeMillis() < startTime + timeout){
            List<Span> spans = listener.drain();
            for (Span span : spans) {
                if(matchedSpans.size() == matchers.length){
                    return;
                }
                for (SpanMatch matcher : matchers) {
                    if(mismatches.get(matcher) != null && mismatches.get(matcher).isEmpty()){
                        continue;
                    }

                    MatchResult result = matcher.matches(span);
                    if(result == MatchResult.SUCCESS){
                        mismatches.put(matcher, new HashMap<>());
                        matchedSpans.add(matcher);
                        break;
                    }

                    Map<Span, MatchResult> misses = mismatches.computeIfAbsent(matcher, k -> new HashMap<>());
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
        for (Map.Entry<SpanMatch, Map<Span, MatchResult>> entry : mismatches.entrySet()) {
            if(entry.getValue().isEmpty()){
                continue;
            }

            mismatchMessage.append("Could not find span for: ").append(entry.getKey()).append("\n");
            for (Map.Entry<Span, MatchResult> misses : entry.getValue().entrySet()) {
                mismatchMessage.append("Span: ").append(misses.getKey().getSpanId()).append("\n");
                mismatchMessage.append(misses.getValue().resultMessage()).append("\n");
            }
        }


        throw new AssertionError("Expected Spans not received:\n" + mismatchMessage);
    }
}
