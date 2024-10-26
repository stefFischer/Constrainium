package at.sfischer.constraints;

import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.model.*;

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
                throw new IllegalArgumentException("Could not apply data (" + values + ") to constraint, remaining: " + result.getClass().getName());
            }
        });

        return new ConstraintResults(this, data, validConstraintData);
    }
}
