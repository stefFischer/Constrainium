package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.TraceNode;

public record Difference<T extends TraceNode<T>>(at.sfischer.traces.otel.differ.Difference.Type type, T referenceSpan, T comparedSpan,
                                                 String message) {
    public enum Type {ADDED, REMOVED, CHANGED}

    @Override
    public String toString() {
        return type + ": " + message;
    }
}
