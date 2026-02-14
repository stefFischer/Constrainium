package at.sfischer.constraints;

import at.sfischer.constraints.miner.ConstraintPolicy;

import java.util.List;
import java.util.Map;

public class ConstraintTemplateFile {

    private final Map<String, ConstraintPolicy> policies;
    private final List<ConstraintTemplate> constraints;
    private final List<GroupDefinition> groups;

    public ConstraintTemplateFile(Map<String, ConstraintPolicy> policies, List<ConstraintTemplate> constraints, List<GroupDefinition> groups) {
        this.policies = policies;
        this.constraints = constraints;
        this.groups = groups;
    }

    public Map<String, ConstraintPolicy> getPolicies() {
        return policies;
    }

    public List<ConstraintTemplate> getConstraints() {
        return constraints;
    }

    public List<GroupDefinition> getGroups() {
        return groups;
    }
}
