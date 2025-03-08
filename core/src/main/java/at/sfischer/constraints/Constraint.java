package at.sfischer.constraints;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;

import java.util.*;

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

    public <T> void applyDataCombinations(Map<Variable, List<Node>> valueCombinations, T dataEntry, ConstraintResults<T> results) {
        List<Variable> variables = new ArrayList<>();
        term.visitNodes((VariableVisitor) variable -> {
            if(variable.getName().equals(ArrayQuantifier.ELEMENT_NAME)){
                return;
            }

            variables.add(variable);
        });

        ApplicationResult result = applyDataRecursive(term, variables, 0, valueCombinations);

        if (result.invalid) {
            results.invalidConstraintData().addDataEntry(dataEntry);
        } else if (result.inapplicable) {
            results.inapplicableConstraintData().addDataEntry(dataEntry);
        } else if (result.valid) {
            results.validConstraintData().addDataEntry(dataEntry);

            // Move data from missingEvidenceConstraintData into validConstraintData, because this result suggests we have enough evidence now.
            results.validConstraintData().addAll(results.missingEvidenceConstraintData());
            results.missingEvidenceConstraintData().clear();
        } else if (result.moreStatisticalEvidenceNeeded) {
            results.missingEvidenceConstraintData().addDataEntry(dataEntry);

            // TODO Maybe we should move all validConstraintData into missingEvidenceConstraintData here? In case we had enough evidence for one lower bound but then a lower data occurred and we don't have enough evidence for that lower bound yet.
        }
    }

    private static class ApplicationResult{
        private boolean valid = false;
        private boolean invalid = false;
        private boolean inapplicable = false;
        private boolean moreStatisticalEvidenceNeeded = false;

        void or(ApplicationResult result){
            this.valid = this.valid || result.valid;
            this.invalid = this.invalid || result.invalid;
            this.inapplicable = this.inapplicable || result.inapplicable;
            this.moreStatisticalEvidenceNeeded = this.moreStatisticalEvidenceNeeded || result.moreStatisticalEvidenceNeeded;
        }
    }

    private static ApplicationResult applyDataRecursive(
            Node currentTerm,
            List<Variable> variables,
            int index,
            Map<Variable, List<Node>> valueCombinations) {

        if (index == variables.size()) {
            // Base case: All placeholders have been replaced
            ApplicationResult applicationResult = new ApplicationResult();
            Node result = currentTerm.evaluate();
            if (result instanceof MoreStatisticalEvidenceNeeded) {
                // More evidence needed.
                applicationResult.moreStatisticalEvidenceNeeded = true;
            } else if (result instanceof BooleanLiteral) {
                // Valid of invalid.
                if(((BooleanLiteral) result).getValue()){
                    applicationResult.valid = true;
                } else {
                    applicationResult.invalid = true;
                }
            } else {
                // Inapplicable.
                applicationResult.inapplicable = true;
            }
            return applicationResult;
        }

        Variable currentVariable = variables.get(index);
        List<Node> possibleValues = valueCombinations.getOrDefault(currentVariable, Collections.emptyList());

        ApplicationResult result = new ApplicationResult();
        if(possibleValues.isEmpty()){
            // Inapplicable if we are missing values for a variable.
            result.inapplicable = true;
        } else {
            for (Node value : possibleValues) {
                Node newTerm = currentTerm.setVariableValue(currentVariable, value);
                ApplicationResult res = applyDataRecursive(newTerm, variables, index + 1, valueCombinations);
                result.or(res);
                if (result.invalid) {
                    break;
                }
            }
        }

        return result;
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
