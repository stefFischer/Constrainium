package at.sfischer.constraints.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Variable implements Node {

    private String name;

    public Variable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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
        return this.name != null;
    }

    @Override
    public List<Node> getChildren() {
        return null;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        Node value = values.get(this);
        if(value != null){
            return value;
        }

        return new Variable(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variable variable = (Variable) o;
        return Objects.equals(name, variable.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Variable{name: " + name + '}';
    }

    public static Variable generateFreshVariable(Set<Variable> avoid) {
        Set<String> existingNames = avoid.stream().map(Variable::getName).collect(Collectors.toSet());

        int index = 1;
        String candidate;
        do {
            candidate = "v" + index++;
        } while (existingNames.contains(candidate));

        return new Variable(candidate);
    }
}
