package at.sfischer.traces.otel.testing;

public class FailExpectedValueResult extends FailResult {
    public FailExpectedValueResult(Object expected, Object actual) {
        super("Expected: <" + expected + ">, but was: <" + actual + ">");
    }
}
