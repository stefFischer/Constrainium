package at.sfischer.constraints.miner;

import at.sfischer.constraints.ConstraintResults;

public interface ConstraintPolicy {

    boolean includeConstraint(ConstraintResults<?> results);
}
