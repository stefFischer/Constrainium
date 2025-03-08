package at.sfischer.constraints;

import at.sfischer.constraints.data.DataCollection;

/**
 * @param constraint          Constraint that was applied on the data.
 * @param data                Data on which the constraint was evaluated on.
 * @param validConstraintData Data for which the constraint evaluates to true.
 */
public record ConstraintResults<T>(
        Constraint constraint,
        DataCollection<T> data,
        DataCollection<T> validConstraintData,
        DataCollection<T> invalidConstraintData,
        DataCollection<T> inapplicableConstraintData,
        DataCollection<T> missingEvidenceConstraintData) {
    public ConstraintResults(Constraint constraint, DataCollection<T> data) {
        this(constraint, data, data.emptyDataCollection());
    }

    public ConstraintResults(Constraint constraint, DataCollection<T> data, DataCollection<T> validConstraintData) {
        this(constraint, data, validConstraintData, data.emptyDataCollection());
    }

    public ConstraintResults(Constraint constraint, DataCollection<T> data, DataCollection<T> validConstraintData, DataCollection<T> inapplicableConstraintData) {
        this(constraint, data, validConstraintData, inapplicableConstraintData, data.emptyDataCollection());
    }

    public ConstraintResults(Constraint constraint, DataCollection<T> data, DataCollection<T> validConstraintData, DataCollection<T> inapplicableConstraintData, DataCollection<T> missingEvidenceConstraintData) {
        this(constraint, data, validConstraintData, data.emptyDataCollection(), inapplicableConstraintData, missingEvidenceConstraintData);
    }

    public ConstraintResults(Constraint constraint, DataCollection<T> data, DataCollection<T> validConstraintData, DataCollection<T> invalidConstraintData, DataCollection<T> inapplicableConstraintData, DataCollection<T> missingEvidenceConstraintData) {
        this.constraint = constraint;
        this.data = data;
        this.validConstraintData = validConstraintData;
        this.invalidConstraintData = invalidConstraintData;
        this.inapplicableConstraintData = inapplicableConstraintData;
        this.missingEvidenceConstraintData = missingEvidenceConstraintData;
    }

    public boolean foundCounterExample() {
        return numberOfViolations() > 0;
    }

    public int numberOfViolations() {
        return invalidConstraintData.numberOfDataEntries();
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
                ", invalidConstraintData=" + invalidConstraintData.size() +
                ", inapplicableConstraintData=" + numberOfInapplicableEntries() +
                '}';
    }
}
