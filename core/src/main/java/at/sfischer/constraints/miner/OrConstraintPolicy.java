package at.sfischer.constraints.miner;

import at.sfischer.constraints.ConstraintResults;

import java.util.List;

public class OrConstraintPolicy implements ConstraintPolicy {

    private List<ConstraintPolicy> policies;

    public OrConstraintPolicy(ConstraintPolicy... policies) {
        this(List.of(policies));
    }

    public OrConstraintPolicy(List<ConstraintPolicy> policies) {
        this.policies = policies;
    }

    @Override
    public boolean includeConstraint(ConstraintResults<?> results) {
        for (ConstraintPolicy policy : policies) {
            if(policy.includeConstraint(results)){
                return true;
            }
        }

        return false;
    }
}
