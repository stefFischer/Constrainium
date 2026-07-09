package at.sfischer.constraints;

import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.model.Node;

import java.util.List;

public class MetamorphicRelationTemplate implements ConstraintConstruct {

    private final String name;
    private final Node transformation;
    private final Node validation;
    private final ConstraintPolicy retentionPolicy;

    public MetamorphicRelationTemplate(String name, Node transformation, Node validation, ConstraintPolicy retentionPolicy) {
        this.name = name;
        this.transformation = transformation;
        this.validation = validation;
        this.retentionPolicy = retentionPolicy;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ConstraintPolicy getRetentionPolicy() {
        return retentionPolicy;
    }

    public Node getTransformation() {
        return transformation;
    }

    public Node getValidation() {
        return validation;
    }

    @Override
    public List<Node> getTerms() {
        return List.of(transformation, validation);
    }
}
