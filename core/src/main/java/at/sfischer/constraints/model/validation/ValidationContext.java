package at.sfischer.constraints.model.validation;

import at.sfischer.constraints.model.Node;

import java.util.ArrayList;
import java.util.List;

public final class ValidationContext {

    private final List<ValidationMessage> messages = new ArrayList<>();

    public void error(Node node, String message) {
        messages.add(new ValidationMessage(
                ValidationSeverity.ERROR,
                message,
                node
        ));
    }

    public void warning(Node node, String message) {
        messages.add(new ValidationMessage(
                ValidationSeverity.WARNING,
                message,
                node
        ));
    }

    public boolean isValid() {
        return messages.stream()
                .noneMatch(m -> m.severity() == ValidationSeverity.ERROR);
    }

    public List<ValidationMessage> getMessages() {
        return messages;
    }
}
