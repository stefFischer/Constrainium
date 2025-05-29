package at.sfischer.constraints.model.pattern;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.Variable;

import java.util.*;

public class NodePatternMatcher {

    public static class PatternNode implements Node {

        private final String clazz;

        private final List<Node> children;

        public PatternNode(String clazz, List<Node> children) {
            this.clazz = clazz;
            this.children = children;
        }

        public PatternNode(String clazz, Node... children) {
            this(clazz, List.of(children));
        }

        @Override
        public Type getReturnType() {
            return TypeEnum.ANY;
        }

        @Override
        public Node evaluate() {
            return this;
        }

        @Override
        public boolean validate() {
            return true;
        }

        @Override
        public List<Node> getChildren() {
            return children;
        }

        @Override
        public Node setVariableValues(Map<Variable, Node> values) {
            return this;
        }
    }

    /**
     * Special node used as a placeholder in pattern trees.
     */
    public static class Placeholder extends Variable {

        public Placeholder(String name) {
            super(name);
        }

        @Override
        public String toString() {
            return "Placeholder(" + getName() + ")";
        }
    }

    /**
     * Attempts to match a pattern tree with a target node tree.
     *
     * @param pattern The pattern node (with potential placeholders).
     * @param target  The actual node to match.
     * @return A map of placeholders to matched nodes, or null if no match.
     */
    public static Map<Placeholder, Node> match(Node pattern, Node target) {
        Map<Placeholder, Node> result = new HashMap<>();
        if (matchRecursive(pattern, target, result)) {
            return result;
        } else {
            return null;
        }
    }

    private static boolean matchRecursive(Node pattern, Node target, Map<Placeholder, Node> bindings) {
        if (pattern instanceof Placeholder placeholder) {
            Node existing = bindings.get(placeholder);
            if (existing == null) {
                bindings.put(placeholder, target);
                return true;
            } else {
                return existing.equals(target);
            }
        }

        if (pattern instanceof PatternNode patternNode) {
            if (!patternNode.clazz.equals(target.getClass().getCanonicalName())) {
                return false;
            }
        } else {
            if (!pattern.getClass().equals(target.getClass())) {
                return false;
            }
        }

        List<Node> patternChildren = pattern.getChildren();
        List<Node> targetChildren = target.getChildren();
        if (patternChildren == targetChildren) {
            return pattern.equals(target);
        }

        if (patternChildren == null || targetChildren == null) {
            return false;
        }

        if (patternChildren.size() != targetChildren.size()) {
            return false;
        }

        for (int i = 0; i < patternChildren.size(); i++) {
            if (!matchRecursive(patternChildren.get(i), targetChildren.get(i), bindings)) {
                return false;
            }
        }

        return true;
    }
}