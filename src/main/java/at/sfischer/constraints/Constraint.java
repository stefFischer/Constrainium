package at.sfischer.constraints;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.VariableVisitor;

import java.util.HashSet;
import java.util.Set;

public record Constraint(Node term) {

    public Constraint {
        if (term.getReturnType() != TypeEnum.BOOLEAN) {
            throw new IllegalArgumentException("Constraint term needs to return boolean, instead of: " + term.getReturnType());
        }

    }

    public <T> ConstraintResults applyData(DataCollection<T> data) {
        DataCollection<T> validConstraintData = data.clone();
        Set<String> variableNames = new HashSet<>();
        term.visitNodes((VariableVisitor) variable -> variableNames.add(variable.getName()));
        data.visitDataEntries(variableNames, (values, dataObject) -> {
            Node valueSetTerm = term.setVariableNameValues(values);
            Node result = valueSetTerm.evaluate();
            if (result instanceof BooleanLiteral) {
                if (!((BooleanLiteral) result).getValue()) {
                    validConstraintData.removeDataEntry(dataObject);
                }
            } else {
                // TODO: Do not panic when a constraint does not apply for data, but instead store a counter for this and use it as an additional statistic to derive a likelihood for a constraint to be true.
                throw new IllegalArgumentException("Could not apply data (" + values + ") to constraint, remaining: " + result.getClass().getName());
            }
        });

        return new ConstraintResults(this, data, validConstraintData);
    }
}
