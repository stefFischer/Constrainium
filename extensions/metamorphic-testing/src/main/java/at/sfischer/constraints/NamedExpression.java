package at.sfischer.constraints;

import at.sfischer.constraints.model.Node;

public record NamedExpression(String name, Node expression) {
}
