package at.sfischer.constraints;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.model.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public record Constraint(Node term) {

    public Constraint {
        if (term.getReturnType() != TypeEnum.BOOLEAN) {
            throw new IllegalArgumentException("Constraint term needs to return boolean, instead of: " + term.getReturnType());
        }

    }

    public <T> ConstraintResults applyData(DataCollection<T> data) {
        DataCollection<T> validConstraintData = data.clone();
        DataCollection<T> missingEvidenceConstraintData = data.emptyDataCollection();
        DataCollection<T> inapplicableConstraintData = data.emptyDataCollection();
        AtomicBoolean evaluatedTrue = new AtomicBoolean(false);

        Set<String> variableNames = new HashSet<>();
        term.visitNodes((VariableVisitor) variable -> variableNames.add(variable.getName()));
        data.visitDataEntries(variableNames, (values, dataObject) -> {
            Node valueSetTerm = term.setVariableNameValues(values);
            Node result = valueSetTerm.evaluate();
            if (result instanceof MoreStatisticalEvidenceNeeded) {
                // Store data, if the constraint is violated by enough data we remove them. If it is never violated these should be in validConstraintData.
                validConstraintData.removeDataEntry(dataObject);
                missingEvidenceConstraintData.addDataEntry(dataObject);
            } else if (result instanceof BooleanLiteral) {
                if (((BooleanLiteral) result).getValue()) {
                    evaluatedTrue.set(true);
                } else {
                    validConstraintData.removeDataEntry(dataObject);
                }
            } else {
                validConstraintData.removeDataEntry(dataObject);
                inapplicableConstraintData.addDataEntry(dataObject);
            }
        });

        // If the constraint was never violated store all in valid data.
        if(evaluatedTrue.get() && validConstraintData.size() + missingEvidenceConstraintData.size() == data.size()){
            validConstraintData.addAll(missingEvidenceConstraintData);
        }

        return new ConstraintResults(this, data, validConstraintData, inapplicableConstraintData);
    }
}
