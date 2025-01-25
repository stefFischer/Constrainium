package at.sfischer.constraints;

import at.sfischer.constraints.data.DataCollection;

/**
 * @param constraint          Constraint that was applied on the data.
 * @param data                Data on which the constraint was evaluated on.
 * @param validConstraintData Data for which the constraint evaluates to true.
 */
public record ConstraintResults(Constraint constraint, DataCollection<?> data, DataCollection<?> validConstraintData, DataCollection<?> inapplicableConstraintData) {
    public ConstraintResults(Constraint constraint, DataCollection<?> data, DataCollection<?> validConstraintData) {
        this(constraint, data, validConstraintData, data.emptyDataCollection());
    }

    public ConstraintResults(Constraint constraint, DataCollection<?> data, DataCollection<?> validConstraintData, DataCollection<?> inapplicableConstraintData) {
        this.constraint = constraint;
        this.data = data;
        this.validConstraintData = validConstraintData;
        this.inapplicableConstraintData = inapplicableConstraintData;
    }

    public boolean foundCounterExample() {
        return numberOfViolations() > 0;
    }

    public int numberOfViolations() {
        return data.numberOfDataEntries() - validConstraintData.numberOfDataEntries() - numberOfInapplicableEntries();
    }

    public int numberOfValidDataEntries(){
        if(validConstraintData == null){
            return 0;
        }

        return validConstraintData.numberOfDataEntries();
    }

    public int numberOfInapplicableEntries() {
        if(inapplicableConstraintData == null){
            return 0;
        }

        return inapplicableConstraintData.numberOfDataEntries();
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
                ", inapplicableConstraintData=" + numberOfInapplicableEntries() +
                '}';
    }
}
