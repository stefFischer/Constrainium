package at.sfischer.constraints;

import java.util.List;

public class GroupDefinition {

    private final String name;
    private final List<ConstraintConstruct> constraints;

    public GroupDefinition(String name,
                           List<ConstraintConstruct> constraints) {
        this.name = name;
        this.constraints = constraints;
    }

    public String getName() {
        return name;
    }


    public List<ConstraintConstruct> getConstraints() {
        return constraints;
    }

    public <T extends ConstraintConstruct> List<T> getConstraints(Class<T> clazz) {
        return constraints.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .toList();
    }
}
