package at.sfischer.traces.otel.testing;

import at.sfischer.traces.otel.matching.FailResult;

public class FailExpectedValueResult extends FailResult {
    public FailExpectedValueResult(Object expected, Object actual) {
        super("Expected: <" + expected + ">, but was: <" + actual + ">");
    }
}
