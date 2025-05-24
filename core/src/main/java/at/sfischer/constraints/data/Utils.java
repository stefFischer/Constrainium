package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;

import java.util.*;

public class Utils {

    public record Path(List<String> segments) {
        public Path(List<String> segments) {
            this.segments = List.copyOf(segments);
        }

        public Path parent() {
            if (segments.isEmpty()) return null;
            return new Path(segments.subList(0, segments.size() - 1));
        }

        public String last() {
            return segments.isEmpty() ? null : segments.get(segments.size() - 1);
        }

        public boolean isEmpty() {
            return segments.isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Path)) return false;
            Path path = (Path) o;
            return segments.equals(path.segments);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(segments);
        }

        @Override
        public String toString() {
            return String.join(".", segments);
        }

        /**
         * Creates a {@link Path} instance from a dot-separated string representation.
         * <p>
         * For example, the input string {@code "a.b.c"} will result in a {@code Path}
         * with segments {@code ["a", "b", "c"]}.
         * </p>
         *
         * @param pathStr the dot-separated string representing a path
         * @return a {@code Path} object containing the segments of the path
         * @throws NullPointerException if {@code pathStr} is {@code null}
         */
        public static Path of(String pathStr) {
            return new Path(List.of(pathStr.split("\\.")));
        }

        public static Path empty() {
            return new Path(List.of());
        }

        public int size() {
            return segments.size();
        }

        public String segment(int i) {
            if (i >= 0 && i < segments.size()) {
                return segments.get(i);
            } else {
                throw new IndexOutOfBoundsException("Index: " + i + ", Size: " + segments.size());
            }
        }

        public Path relativize(Path prefix) {
            if (prefix == null || prefix.size() > this.size()) {
                throw new IllegalArgumentException("Prefix is longer than the path or null.");
            }

            for (int i = 0; i < prefix.size(); i++) {
                if (!this.segment(i).equals(prefix.segment(i))) {
                    throw new IllegalArgumentException("Prefix does not match the beginning of this path.");
                }
            }

            return new Path(this.segments.subList(prefix.size(), this.size()));
        }

        public Path append(String segment) {
            List<String> newSegments = new ArrayList<>(this.segments);
            newSegments.add(segment);
            return new Path(newSegments);
        }
    }

    public static class Group {
        private Path path;
        private Variable variable;
        private final Map<Path, Group> children;

        public Group(Path path) {
            this(path, null, new HashMap<>());
        }

        public Group(Path path, Map<Path, Group> children) {
            this(path, null, children);
        }

        public Group(Path path, Variable variable, Map<Path, Group> children) {
            this.path = path;
            this.variable = variable;
            this.children = children;
        }

        public Path getPath() {
            return path;
        }

        public Variable getVariable() {
            return variable;
        }

        public Map<Path, Group> getChildren() {
            return children;
        }

        private void addVariable(Variable variable, Path fullPath, int index) {
            if (index >= fullPath.size()) {
                this.variable = variable;
                return;
            }
            Path childPath = new Path(fullPath.segments().subList(0, index + 1));
            Group child = children.computeIfAbsent(childPath, Group::new);
            child.addVariable(variable, fullPath, index + 1);
        }

        @Override
        public String toString() {
            return toString("");
        }

        private String toString(String indent){
            StringBuilder sb =new StringBuilder();
            sb.append(indent).append(path);
            if(variable != null){
                sb.append(": ").append(variable);
            }
            if(!children.isEmpty()){
                for (Group child : children.values()) {
                    sb.append("\n").append(child.toString(indent + "\t"));
                }
            }

            return sb.toString();
        }
    }

    public static Map<Path, Group> groupByCollapsedHierarchy(Set<Variable> variables) {
        Map<Path, Group> rootGroups = new HashMap<>();

        for (Variable variable : variables) {
            Path fullPath = Path.of(variable.getName());
            Path currentPrefix = new Path(List.of(fullPath.segment(0)));

            // Find or create top-level group root
            Group root = rootGroups.computeIfAbsent(currentPrefix, Group::new);
            root.addVariable(variable, fullPath, 1);
        }

        // Collapse groups where there is only one child and no variables
        return collapseGroups(rootGroups);
    }

    private static Map<Path, Group> collapseGroups(Map<Path, Group> groups) {
        Map<Path, Group> result = new HashMap<>();

        for (Group group : groups.values()) {
            Group collapsed = collapseGroup(group);
            result.put(collapsed.getPath(), collapsed);
        }

        return result;
    }

    private static Group collapseGroup(Group group) {
        while (group.variable == null && group.children.size() == 1) {
            group = group.children.values().iterator().next();
        }

        Group newGroup = new Group(group.path);
        newGroup.variable = group.variable;

        for (Group child : group.children.values()) {
            Group collapsedChild = collapseGroup(child);
            // Relativize paths.
            collapsedChild.path = collapsedChild.getPath().relativize(group.path);
            newGroup.children.put(collapsedChild.getPath(), collapsedChild);
        }

        return newGroup;
    }

    /**
     * Collects combinations of values for the given set of {@link Variable}s from the provided {@link DataObject}.
     * <p>
     * The method groups the variables by their lowest shared complex-type object context using
     * a collapsed hierarchy strategy (via {@code groupByCollapsedHierarchy}). It then collects
     * value combinations such that each combination includes only values that co-occur within the
     * same instance of the shared complex object. Finally, it combines the groups to produce
     * all valid combinations across different object groupings.
     * </p>
     *
     * <p>
     * This is useful for evaluating constraints or logical terms where variables must be
     * bound to values that appear together in the same object structure.
     * </p>
     *
     * @param dao the root {@code DataObject} from which values are to be extracted
     * @param targetVariables the set of variables whose values should be collected
     * @return a list of value combinations, where each combination is represented as a map
     *         from {@code Variable} to {@code Node}
     * @throws NullPointerException if {@code dao} or {@code targetVariables} is {@code null}
     */
    public static List<Map<Variable, Node>> collectValueCombinations(
            DataObject dao,
            Set<Variable> targetVariables
    ){
        Map<Path, Group> groups = Utils.groupByCollapsedHierarchy(targetVariables);
        return collectValueCombinations(dao, groups);
    }

    public static List<Map<Variable, Node>>  collectValueCombinations(
            DataObject dao,
            Map<Path, Group> groups
    ){
        List<Map<Variable, Node>> values = new LinkedList<>();
        groups.forEach((path, group) -> {
            List<Map<Variable, Node>> childValues = collectValueCombinations(dao, group);
            addValueCombinations(values, childValues);
        });

        return values;
    }

    private static List<Map<Variable, Node>> collectValueCombinations(
            DataObject dao,
            Group group
    ){
        List<Map<Variable, Node>> values = new LinkedList<>();
        Variable var = group.getVariable();
        List<Value<?>> variableValues = dao.getValues(group.getPath());

        if(var != null){
            if(variableValues != null){
                addValueCombinations(values, var, variableValues);
            }
        }

        Map<Path, Group> children = group.getChildren();
        if(variableValues != null){
            for (Value<?> variableValue : variableValues) {
                if(variableValue instanceof ComplexValue){
                    DataObject variableDao = ((ComplexValue)variableValue).getValue();
                    List<Map<Variable, Node>> childValues = collectValueCombinations(variableDao, children);
                    values.addAll(childValues);
                } else if(variableValue instanceof ArrayValues<?>){
                    if(((ArrayValues<?>)variableValue).getElementType() == TypeEnum.COMPLEXTYPE){
                        for (Value<?> value : ((ArrayValues<?>) variableValue).getValue()) {
                            if(value instanceof ComplexValue){
                                DataObject variableDao = ((ComplexValue)value).getValue();
                                List<Map<Variable, Node>> childValues = collectValueCombinations(variableDao, children);
                                values.addAll(childValues);
                            }
                        }
                    }
                }
            }
        }

        return values;
    }

    private static void addValueCombinations(List<Map<Variable, Node>> values, Variable variable, List<Value<?>> variableValues){
        if(variableValues.isEmpty()){
            return;
        }
        if(values.isEmpty()){
            for (Value<?> variableValue : variableValues) {
                Map<Variable, Node> combination = new HashMap<>();
                combination.put(variable, variableValue);
                values.add(combination);
            }
        } else {
            List<Map<Variable, Node>> oldValues = new LinkedList<>(values);
            values.clear();
            for (Value<?> variableValue : variableValues) {
                for (Map<Variable, Node> oldCombination : oldValues) {
                    Map<Variable, Node> combination = new HashMap<>(oldCombination);
                    combination.put(variable, variableValue);
                    values.add(combination);
                }
            }
        }
    }

    public static void addValueCombinations(List<Map<Variable, Node>> values, List<Map<Variable, Node>> toAdd){
        if(toAdd.isEmpty()){
            return;
        }
        if(values.isEmpty()){
            values.addAll(toAdd);
        } else {
            List<Map<Variable, Node>> oldValues = new LinkedList<>(values);
            values.clear();
            for (Map<Variable, Node> nodeMapToAdd : toAdd) {
                for (Map<Variable, Node> oldCombination : oldValues) {
                    Map<Variable, Node> combination = new HashMap<>(oldCombination);
                    combination.putAll(nodeMapToAdd);
                    values.add(combination);
                }
            }
        }
    }
}
