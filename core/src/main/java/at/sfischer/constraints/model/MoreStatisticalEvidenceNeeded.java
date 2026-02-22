package at.sfischer.constraints.model;

import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MoreStatisticalEvidenceNeeded implements EvaluationResultNode {

    public static final MoreStatisticalEvidenceNeeded INSTANCE = new MoreStatisticalEvidenceNeeded();

    private MoreStatisticalEvidenceNeeded() {
    }


    @Override
    public Node evaluate() {
        return this;
    }

    @Override
    public void validate(ValidationContext context) {
    }

    @Override
    public List<Node> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return this;
    }
}
