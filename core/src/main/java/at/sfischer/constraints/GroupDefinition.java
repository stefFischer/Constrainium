package at.sfischer.constraints;

import java.util.List;

public class GroupDefinition {

    private final String name;
    private final List<ConstraintTemplate> constraints;

    public GroupDefinition(String name,
                           List<ConstraintTemplate> constraints) {
        this.name = name;
        this.constraints = constraints;
    }

    public String getName() {
        return name;
    }


    public List<ConstraintTemplate> getConstraints() {
        return constraints;
    }
}
