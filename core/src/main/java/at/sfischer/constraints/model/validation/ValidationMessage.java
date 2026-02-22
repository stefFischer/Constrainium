package at.sfischer.constraints.model.validation;

import at.sfischer.constraints.model.Node;

public record ValidationMessage(ValidationSeverity severity, String message, Node node) {

    @Override
    public String toString() {
        return "ValidationMessage{" +
                "severity=" + severity +
                ", message='" + message + '\'' +
                ", node=" + node +
                '}';
    }
}
