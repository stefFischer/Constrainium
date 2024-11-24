package at.sfischer.constraints.model;

public interface NodeVisitor {
    /**
     * Is called when a node is visited by a visitor.
     *
     * @param node that is visited.
     * @return true if children should be also visited, false if children of node should be skipped.
     */
    boolean visitNode(Node node);
}
