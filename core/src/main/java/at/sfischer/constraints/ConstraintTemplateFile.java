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
    private final List<ConstraintConstruct> constraints;
    private final List<GroupDefinition> groups;

    public ConstraintTemplateFile(Map<String, ConstraintPolicy> policies, List<ConstraintConstruct> constraints, List<GroupDefinition> groups) {
        this.policies = policies;
        this.constraints = constraints;
        this.groups = groups;
    }

    public Map<String, ConstraintPolicy> getPolicies() {
        return policies;
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

    public List<GroupDefinition> getGroups() {
        return groups;
    }

    public Map<ConstraintConstruct, ValidationContext> removeInvalidConstraints(){
        Map<ConstraintConstruct, ValidationContext> removed = new HashMap<>();
        removeInvalidConstraintsFromList(this.constraints, removed);

        for (GroupDefinition group : this.groups) {
            removeInvalidConstraintsFromList(group.getConstraints(), removed);
        }

        return removed;
    }

    private static void removeInvalidConstraintsFromList(final List<ConstraintConstruct> constraints, final Map<ConstraintConstruct, ValidationContext> removed){
        constraints.removeIf(constraint -> {
            ValidationContext context = new ValidationContext();
            constraint.getTerms().forEach(term -> term.validate(context));
            if(context.isValid()){
                return false;
            }

            LOGGER.info("Remove invalid constraint {}, because of validation errors: {}", constraint, context.getMessages());
            removed.put(constraint, context);
            return true;
        });
    }
}
