package at.sfischer.constraints;

import at.sfischer.constraints.data.DataCollection;

/**
 * @param constraint          Constraint that was applied on the data.
 * @param data                Data on which the constraint was evaluated on.
 * @param validConstraintData Data for which the constraint evaluates to true.
 */
public record ConstraintResults(Constraint constraint, DataCollection<?> data, DataCollection<?> validConstraintData) {

    public boolean foundCounterExample() {
        return numberOfViolations() > 0;
    }

    public int numberOfViolations() {
        return data.numberOfDataEntries() - validConstraintData.numberOfDataEntries();
    }

    public double applicationRate() {
        return (double) validConstraintData.numberOfDataEntries() / data.numberOfDataEntries();
    }

    public double violationRate() {
        return 1.0 - applicationRate();
    }

    @Override
    public String toString() {
        return "ConstraintResults{" +
                "constraint=" + constraint +
                ", data=" + data.size() +
                ", validConstraintData=" + validConstraintData.size() +
                '}';
    }
}
