package at.sfischer.constraints.miner;

import at.sfischer.constraints.ConstraintResults;

public class MinApplicationsPolicy implements ConstraintPolicy {

    private final int minNumberOfValidData;

    public MinApplicationsPolicy(int minNumberOfValidData) {
        this.minNumberOfValidData = minNumberOfValidData;
    }

    @Override
    public boolean includeConstraint(ConstraintResults<?> results) {
        return results.numberOfValidDataEntries() >= minNumberOfValidData;
    }
}
