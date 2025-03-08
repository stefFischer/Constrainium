package at.sfischer.constraints.miner;

import at.sfischer.constraints.ConstraintResults;

public class NoViolationsPolicy implements ConstraintPolicy {

    @Override
    public boolean includeConstraint(ConstraintResults<?> results) {
        return !results.foundCounterExample();
    }
}
