package at.sfischer.constraints.miner;

import at.sfischer.constraints.ConstraintResults;

import java.util.List;

public class AndConstraintPolicy implements ConstraintPolicy {

    private List<ConstraintPolicy> policies;

    public AndConstraintPolicy(ConstraintPolicy... policies) {
        this(List.of(policies));
    }

    public AndConstraintPolicy(List<ConstraintPolicy> policies) {
        this.policies = policies;
    }

    @Override
    public boolean includeConstraint(ConstraintResults<?> results) {
        for (ConstraintPolicy policy : policies) {
            if(!policy.includeConstraint(results)){
                return false;
            }
        }

        return true;
    }
}
