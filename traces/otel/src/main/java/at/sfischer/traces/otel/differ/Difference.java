package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.Span;

public record Difference(at.sfischer.traces.otel.differ.Difference.Type type, Span referenceSpan, Span comparedSpan,
                         String message) {
    public enum Type {ADDED, REMOVED, CHANGED}

    @Override
    public String toString() {
        return type + ": " + message;
    }
}
