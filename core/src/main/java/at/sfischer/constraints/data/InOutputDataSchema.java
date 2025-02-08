package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.objects.Reference;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.util.*;

public class InOutputDataSchema<SCHEMA extends DataSchema> extends DataSchema {

    private final SCHEMA inputSchema;

    private final SCHEMA outputSchema;

    public InOutputDataSchema(SCHEMA inputSchema, SCHEMA outputSchema) {
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
    }

    @Override
    public void fillSchemaWithConstraints(Node term) {
        fillSchemaWithConstraintsFromTwoSchemas(term, this.inputSchema.getDataSchemaEntries(), this.outputSchema.getDataSchemaEntries(), 0);
    }

    private <DS extends DataSchema> void fillSchemaWithConstraintsFromTwoSchemas(Node term,
                                                         Collection<DataSchemaEntry<DS>> schema1,
                                                         Collection<DataSchemaEntry<DS>> schema2,
                                                         int recursiveCount) {

        Map<Variable, Type> placeholderTypes = term.inferVariableTypes();
        Map<Variable, List<DataSchemaEntry<DS>>> entries1 = new HashMap<>();
        Map<Variable, List<DataSchemaEntry<DS>>> entries2 = new HashMap<>();

        // Find matching entries for each placeholder from both schemas
        for (Map.Entry<Variable, Type> placeholder : placeholderTypes.entrySet()) {
            Type expectedType = placeholder.getValue();

            // Collect matches for this placeholder from both schemas
            entries1.put(placeholder.getKey(), findMatchingEntries(schema1, schema2, placeholder.getKey(), expectedType, term, recursiveCount));
            entries2.put(placeholder.getKey(), findMatchingEntries(schema2, schema1, placeholder.getKey(), expectedType, term, recursiveCount));
        }


        // Generate all valid combinations ensuring both schemas contribute
        List<Map<Variable, DataSchemaEntry<DS>>> validCombinations = generateValidCrossSchemaCombinations(entries1, entries2);

        // Fill term with the valid combinations
        DataSchema.fillSchemaWithConstraint(term, validCombinations, new HighestEntryFromSchemaSelector<>(this.outputSchema));
    }

    private <DS extends DataSchema> List<DataSchemaEntry<DS>> findMatchingEntries(
            Collection<DataSchemaEntry<DS>> schema,
            Collection<DataSchemaEntry<DS>> otherSchema,
            Variable variable,
            Type valueType,
            Node term,
            int recursiveCount) {
        List<DataSchemaEntry<DS>> matches = new ArrayList<>();
        for (DataSchemaEntry<DS> entry : schema) {
            if (valueType.canAssignTo(entry.type)) {
                matches.add(entry);
            } else if(entry.type instanceof ArrayType){
                if (valueType.canAssignTo(((ArrayType) entry.type).elementType())) {
                    if(recursiveCount <= 0) {
                        Node replacedTerm = new ForAll(variable, term.setVariableValue(variable, new Variable(ArrayQuantifier.ELEMENT_NAME)));
                        fillSchemaWithConstraintsFromTwoSchemas(replacedTerm, schema, otherSchema, recursiveCount + 1);
                    }
                    continue;
                } else if(((ArrayType) entry.type).elementType() == TypeEnum.COMPLEXTYPE){
                    if(recursiveCount <= 0) {
                        // Find values inside complex value that can be inserted into the term.
                        List<DataSchemaEntry<DS>> innerMatches = findMatchingEntries(entry.dataSchema.getDataSchemaEntries(), otherSchema, variable, valueType, term, recursiveCount);
                        for (DataSchemaEntry<DS> innerMatch : innerMatches) {
                            Node replacedTerm = new ForAll(variable, term.setVariableValue(variable, new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral(innerMatch.getQualifiedName().substring(entry.getQualifiedName().length() + 1)))));
                            fillSchemaWithConstraintsFromTwoSchemas(replacedTerm, schema, otherSchema, recursiveCount + 1);
                        }
                    }
                    continue;
                }
            }
            if (entry.dataSchema != null) {
                matches.addAll(findMatchingEntries(entry.dataSchema.getDataSchemaEntries(), otherSchema, variable, valueType, term, recursiveCount));
            }
        }

        return matches;
    }

    @Override
    public <DS extends DataSchema, T> EvaluationResults<DS, T> evaluate(DataCollection<T> data) {
        EvaluationResults<DS, T> evaluationResults = new EvaluationResults<>();
        data.visitDataEntries((values, dataEntry) -> {
            if(!(dataEntry instanceof Pair)){
                return;
            }

            //noinspection unchecked
            evaluateDataObject((Pair<DataObject, DataObject>)dataEntry, dataEntry, data, evaluationResults);
        });

        return evaluationResults;
    }

    public <DS extends DataSchema, T> void evaluateDataObject(Pair<DataObject, DataObject> dao, T dataEntry, DataCollection<T> data, EvaluationResults<DS, T> evaluationResults){
        Map<Variable, Node> values = new HashMap<>();

        Map<DataSchemaEntry<DS>, Set<Constraint>> constraints = new HashMap<>();
        Map<DataSchemaEntry<DS>, Set<Constraint>> potentialConstraints = new HashMap<>();

        evaluateDataObject(inputSchema.getDataSchemaEntries(), dao.getValue0(), dataEntry, values, constraints, potentialConstraints, evaluationResults);
        evaluateDataObject(outputSchema.getDataSchemaEntries(), dao.getValue1(), dataEntry, values, constraints, potentialConstraints, evaluationResults);

        constraints.forEach((k, v) -> {
            if(v == null || v.isEmpty()){
                return;
            }

            for (Constraint constraint : v) {
                ConstraintResults<T> constraintResults = evaluationResults.getConstraintResults(k, constraint, data);
                constraint.applyData(values, dataEntry, constraintResults);
            }
        });

        potentialConstraints.forEach((k, v) -> {
            if(v == null || v.isEmpty()){
                return;
            }

            for (Constraint constraint : v) {
                ConstraintResults<T> constraintResults = evaluationResults.getPotentialConstraintResults(k, constraint, data);
                constraint.applyData(values, dataEntry, constraintResults);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<DataSchemaEntry<SCHEMA>> getDataSchemaEntries() {
        Set<DataSchemaEntry<SCHEMA>> dataSchemaEntries = new HashSet<>();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InOutputDataSchema<?> schema = (InOutputDataSchema<?>) o;
        return Objects.equals(inputSchema, schema.inputSchema) && Objects.equals(outputSchema, schema.outputSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputSchema, outputSchema);
    }

    @Override
    public String toString() {
        return "input:\n" +
                inputSchema +
                "output:\n" +
                outputSchema;
    }
}
