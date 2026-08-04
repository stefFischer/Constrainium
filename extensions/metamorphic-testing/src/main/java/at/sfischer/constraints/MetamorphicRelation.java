package at.sfischer.constraints;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.Utils;
import at.sfischer.constraints.model.BooleanLiteral;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Variable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MetamorphicRelation implements IConstraint {

    private final MetamorphicRelationTemplate derivedFrom;

    private final Node transformation;
    private final Node validation;

    public MetamorphicRelation(Node transformation, Node validation) {
        this(null, transformation, validation);
    }

    public MetamorphicRelation(MetamorphicRelationTemplate derivedFrom, Node transformation, Node validation) {
        this.derivedFrom = derivedFrom;
        this.transformation = transformation;
        this.validation = validation;
    }

    @Override
    public ConstraintConstruct derivedFrom() {
        return derivedFrom;
    }

    public Node getTransformation() {
        return transformation;
    }

    public Node getValidation() {
        return validation;
    }

    @Override
    public <T> void evaluate(DataObject dao, T dataEntry, ConstraintResults<T> constraintResults) {
        Set<Variable> constraintVariables = validation.findInvolvedVariables();
        List<Map<Variable, Node>> valueCombinations = Utils.collectValueCombinations(dao, constraintVariables);
        applyDataCombinations(valueCombinations, dataEntry, constraintResults);
    }

    public <T> void applyDataCombinations(List<Map<Variable, Node>> valueCombinations, T dataEntry, ConstraintResults<T> results) {
        ApplicationResult result = ApplicationResult.applyData(validation, valueCombinations, BooleanLiteral.class, BooleanLiteral::getValue);
        ApplicationResult.updateConstraintResults(result, dataEntry, results);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MetamorphicRelation that = (MetamorphicRelation) o;
        return Objects.equals(transformation, that.transformation) && Objects.equals(validation, that.validation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transformation, validation);
    }

    @Override
    public String toString() {
        return transformation + " -> " + validation;
    }
}
