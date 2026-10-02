package at.sfischer.constraints;

import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.model.Node;

import java.util.List;
import java.util.stream.Stream;

public class MetamorphicRelationSetTemplate implements ConstraintConstruct {

    private final String name;
    private final List<NamedExpression> transformations;
    private final List<NamedExpression> validations;
    private final ConstraintPolicy retentionPolicy;

    public MetamorphicRelationSetTemplate(
            String name,
            List<NamedExpression> transformations,
            List<NamedExpression> validations,
            ConstraintPolicy retentionPolicy) {

        this.name = name;
        this.transformations = List.copyOf(transformations);
        this.validations = List.copyOf(validations);
        this.retentionPolicy = retentionPolicy;
    }

    public String getName() {
        return name;
    }

    public List<NamedExpression> getTransformations() {
        return transformations;
    }

    public List<NamedExpression> getValidations() {
        return validations;
    }

    public ConstraintPolicy getRetentionPolicy() {
        return retentionPolicy;
    }

    @Override
    public List<Node> getTerms() {
        return Stream.concat(
                transformations.stream().map(NamedExpression::expression),
                validations.stream().map(NamedExpression::expression)
        ).toList();
    }
}
