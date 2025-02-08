package at.sfischer.constraints;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.model.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record Constraint(Node term) {

    public Constraint {
        if (term.getReturnType() != TypeEnum.BOOLEAN) {
            throw new IllegalArgumentException("Constraint term needs to return boolean, instead of: " + term.getReturnType());
        }

    }

    public <T> ConstraintResults<T> applyData(DataCollection<T> data) {
        DataCollection<T> validConstraintData = data.emptyDataCollection();
        DataCollection<T> missingEvidenceConstraintData = data.emptyDataCollection();
        DataCollection<T> inapplicableConstraintData = data.emptyDataCollection();

        ConstraintResults<T> results = new ConstraintResults<>(this, data, validConstraintData, inapplicableConstraintData, missingEvidenceConstraintData);

        Set<String> variableNames = new HashSet<>();
        term.visitNodes((VariableVisitor) variable -> variableNames.add(variable.getName()));
        data.visitDataEntries(variableNames, (values, dataEntry) -> {
            applyNamedData(values, dataEntry, results);
        });

        return results;
    }

    public <T> void applyNamedData(Map<String, Node> values, T dataEntry, ConstraintResults<T> results) {
        Map<Variable, Node> variableValues = new HashMap<>();
        for (Map.Entry<String, Node> entry : values.entrySet()) {
            variableValues.put(new Variable(entry.getKey()), entry.getValue());
        }

        applyData(variableValues, dataEntry, results);
    }

    public <T> void applyData(Map<Variable, Node> values, T dataEntry, ConstraintResults<T> results) {
        Node valueSetTerm = term.setVariableValues(values);
        Node result = valueSetTerm.evaluate();
        if (result instanceof MoreStatisticalEvidenceNeeded) {
            results.missingEvidenceConstraintData().addDataEntry(dataEntry);
        } else if (result instanceof BooleanLiteral) {
            if (((BooleanLiteral) result).getValue()) {
                results.validConstraintData().addDataEntry(dataEntry);
                results.validConstraintData().addAll(results.missingEvidenceConstraintData());
                results.missingEvidenceConstraintData().clear();
            }
        } else {
            results.inapplicableConstraintData().addDataEntry(dataEntry);
        }
    }
}
