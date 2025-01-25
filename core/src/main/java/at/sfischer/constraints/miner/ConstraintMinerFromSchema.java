package at.sfischer.constraints.miner;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.DataSchema;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.Operator;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstraintMinerFromSchema implements ConstraintMiner {

    private final DataSchema schema;

    public ConstraintMinerFromSchema(DataSchema schema) {
        this.schema = schema;
    }

    @Override
    public Set<Constraint> getPossibleConstraints(Set<Node> terms) {
        Set<Constraint> possibleConstraints = new HashSet<>();
        for (Node term : terms) {
            Map<Variable, Type> variableTypes = term.inferVariableTypes();
            List<Node> replacedTerms = schema.applyDataToTerms(term, variableTypes);
            for (Node replacedTerm : replacedTerms) {
                // Validate terms to remove nonsensical ones, like a < a, that will be either always true or always false.
                Node evaluated = replacedTerm.evaluate();
                // If the term that remains is no longer an operator, i.e., is a literal or a simple variable, the constraint is not useful.
                if(!(evaluated instanceof Operator)){
                    continue;
                }

                Constraint constraint = new Constraint(replacedTerm);
                possibleConstraints.add(constraint);
            }
        }

        return possibleConstraints;
    }
}
