package at.sfischer.constraints;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.Utils;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;

import java.util.*;

public record Constraint(Node term, ConstraintConstruct derivedFrom) implements IConstraint {

    public Constraint {
        if (term.getReturnType() != TypeEnum.BOOLEAN) {
            throw new IllegalArgumentException("Constraint term needs to return boolean, instead of: " + term.getReturnType());
        }
    }

    public Constraint(Node term) {
        this(term, null);
    }

    @Override
    public ConstraintConstruct derivedFrom() {
        return derivedFrom;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Constraint that = (Constraint) o;
        return Objects.equals(term, that.term);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(term);
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

    @Override
    public <T> void evaluate(DataObject dao, T dataEntry, ConstraintResults<T> constraintResults) {
        Set<Variable> constraintVariables = term.findInvolvedVariables();
        List<Map<Variable, Node>> valueCombinations = Utils.collectValueCombinations(dao, constraintVariables);
        applyDataCombinations(valueCombinations, dataEntry, constraintResults);
    }

    public <T> void applyDataCombinations(List<Map<Variable, Node>> valueCombinations, T dataEntry, ConstraintResults<T> results) {
        ApplicationResult result = ApplicationResult.applyData(term, valueCombinations, BooleanLiteral.class, BooleanLiteral::getValue);
        ApplicationResult.updateConstraintResults(result, dataEntry, results);
    }

    public <T> void applyData(Map<Variable, Node> values, T dataEntry, ConstraintResults<T> results) {
        Node valueSetTerm = term.setVariableValues(values);
        Node result = valueSetTerm.evaluate();
        if (result instanceof MoreStatisticalEvidenceNeeded) {
            results.missingEvidenceConstraintData().addDataEntry(dataEntry);

            // TODO Maybe we should move all validConstraintData into missingEvidenceConstraintData here? In case we had enough evidence for one lower bound but then a lower data occurred and we don't have enough evidence for that lower bound yet.
        } else if (result instanceof BooleanLiteral) {
            if (((BooleanLiteral) result).getValue()) {
                results.validConstraintData().addDataEntry(dataEntry);

                // Move data from missingEvidenceConstraintData into validConstraintData, because this result suggests we have enough evidence now.
                results.validConstraintData().addAll(results.missingEvidenceConstraintData());
                results.missingEvidenceConstraintData().clear();
            } else {
                results.invalidConstraintData().addDataEntry(dataEntry);
            }
        } else {
            results.inapplicableConstraintData().addDataEntry(dataEntry);
        }
    }
}
