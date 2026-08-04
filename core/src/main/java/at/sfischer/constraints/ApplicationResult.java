package at.sfischer.constraints;

import at.sfischer.constraints.model.MoreStatisticalEvidenceNeeded;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ApplicationResult {
    private boolean valid = false;
    private boolean invalid = false;
    private boolean inapplicable = false;
    private boolean moreStatisticalEvidenceNeeded = false;

    public boolean isValid() {
        return valid;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public boolean isInapplicable() {
        return inapplicable;
    }

    public boolean isMoreStatisticalEvidenceNeeded() {
        return moreStatisticalEvidenceNeeded;
    }

    public void or(ApplicationResult result){
        this.valid = this.valid || result.valid;
        this.invalid = this.invalid || result.invalid;
        this.inapplicable = this.inapplicable || result.inapplicable;
        this.moreStatisticalEvidenceNeeded = this.moreStatisticalEvidenceNeeded || result.moreStatisticalEvidenceNeeded;
    }

    public static <T extends Node> ApplicationResult applyData(
            Node term,
            List<Map<Variable, Node>> valueCombinations,
            Class<T> expectedType,
            Predicate<T> success
    ) {
        ApplicationResult applicationResult = new ApplicationResult();
        if (valueCombinations.isEmpty()) {
            applicationResult.inapplicable = true;
            return applicationResult;
        }

        for (Map<Variable, Node> valueCombination : valueCombinations) {
            Node result = term.setVariableValues(valueCombination).evaluate();
            if (result instanceof MoreStatisticalEvidenceNeeded) {
                applicationResult.moreStatisticalEvidenceNeeded = true;
                continue;
            }

            if (!expectedType.isInstance(result)) {
                applicationResult.inapplicable = true;
                continue;
            }

            T value = expectedType.cast(result);
            if (success.test(value)) {
                applicationResult.valid = true;
            } else {
                applicationResult.invalid = true;
                break;
            }
        }

        return applicationResult;
    }

    public static <T> void updateConstraintResults(ApplicationResult result, T dataEntry, ConstraintResults<T> results){
        if (result.isInvalid()) {
            results.invalidConstraintData().addDataEntry(dataEntry);
        } else if (result.isValid()) {
            results.validConstraintData().addDataEntry(dataEntry);

            // Move data from missingEvidenceConstraintData into validConstraintData, because this result suggests we have enough evidence now.
            results.validConstraintData().addAll(results.missingEvidenceConstraintData());
            results.missingEvidenceConstraintData().clear();
        } else if (result.isInapplicable()) { // TODO Maybe we should make it configurable to decide the behavior here? If valid or inapplicable should be preferred if we have multiple value combinations and in some not all values are set.
            results.inapplicableConstraintData().addDataEntry(dataEntry);
        } else if (result.isMoreStatisticalEvidenceNeeded()) {
            results.missingEvidenceConstraintData().addDataEntry(dataEntry);

            // TODO Maybe we should move all validConstraintData into missingEvidenceConstraintData here? In case we had enough evidence for one lower bound but then a lower data occurred and we don't have enough evidence for that lower bound yet.
        }
    }
}
