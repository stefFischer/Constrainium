package at.sfischer.constraints.data;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.Variable;
import org.javatuples.Triplet;

import java.util.*;

public class InOutputDataSchema<T extends DataSchema> extends DataSchema {

    private final T inputSchema;

    private final T outputSchema;

    public InOutputDataSchema(T inputSchema, T outputSchema) {
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
    }

    @Override
    protected Collection<DataSchemaEntry<?>> getDataSchemaEntries() {
        Set<DataSchemaEntry<?>> dataSchemaEntries = new HashSet<>();
        dataSchemaEntries.addAll(inputSchema.getDataSchemaEntries());
        dataSchemaEntries.addAll(outputSchema.getDataSchemaEntries());
        return dataSchemaEntries;
    }

    @Override
    public List<Node> applyDataToTerms(Node term, Map<Variable, Type> variableTypes) {
        List<Triplet<Node, Set<Variable>, Set<Node>>> termsToAssign = new LinkedList<>();
        termsToAssign.add(new Triplet<>(term, new HashSet<>(variableTypes.keySet()), new HashSet<>()));
        DataSchema.findAssignableFields(termsToAssign, variableTypes, inputSchema.getDataSchemaEntries(), variableNodeProvider);
        termsToAssign.removeIf(pair -> pair.getValue1().isEmpty());
        // If there are no terms for which input data could be used we can end here.
        if(termsToAssign.isEmpty()){
            return Collections.emptyList();
        }

        List<Triplet<Node, Set<Variable>, Set<Node>>> nextTermsToAssign = new LinkedList<>();
        termsToAssign.forEach(t -> nextTermsToAssign.add(new Triplet<>(t.getValue0(), t.getValue1(), new HashSet<>())));
        DataSchema.findAssignableFields(nextTermsToAssign, variableTypes, outputSchema.getDataSchemaEntries(), variableNodeProvider);

        List<Node> terms = new LinkedList<>();
        for (Triplet<Node, Set<Variable>, Set<Node>> triplet : nextTermsToAssign) {
            // Check if all variables have been assigned a field.
            if(triplet.getValue1().isEmpty()){
                terms.add(triplet.getValue0());
            }
        }

        return terms;
    }
}
