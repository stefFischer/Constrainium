package at.sfischer.constraints;

import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstraintTemplateFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintTemplateFile.class);

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

    public Map<ConstraintTemplate, ValidationContext> removeInvalidConstraints(){
        Map<ConstraintTemplate, ValidationContext> removed = new HashMap<>();
        removeInvalidConstraintsFromList(this.constraints, removed);

        for (GroupDefinition group : this.groups) {
            removeInvalidConstraintsFromList(group.getConstraints(), removed);
        }

        return removed;
    }

    private static void removeInvalidConstraintsFromList(final List<ConstraintTemplate> constraints, final Map<ConstraintTemplate, ValidationContext> removed){
        constraints.removeIf(constraint -> {
            ValidationContext context = new ValidationContext();
            constraint.getTerm().validate(context);
            if(context.isValid()){
                return false;
            }

            LOGGER.info("Remove invalid constraint {}, because of validation errors: {}", constraint, context.getMessages());
            removed.put(constraint, context);
            return true;
        });
    }
}
