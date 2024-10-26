package at.sfischer.constraints.model;

public interface VariableVisitor extends NodeVisitor {

    @Override
    default boolean visitNode(Node node) {
        if (node instanceof Variable){
            visitVariable((Variable)node);
            return false;
        }

        return true;
    }

    void visitVariable(Variable variable);
}
