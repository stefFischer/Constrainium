package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.miner.AndConstraintPolicy;
import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.objects.Reference;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.util.*;

public abstract class DataSchema {

    protected interface FieldNodeProvider{
        Node generateNode(Pair<String, Type> field);
    }

    protected static final FieldNodeProvider variableNodeProvider = field -> new Variable(field.getValue0());
    protected static final FieldNodeProvider arrayElementProvider = field -> new Variable(ArrayQuantifier.ELEMENT_NAME);
    protected static final FieldNodeProvider arrayDereferenceProvider = field -> new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral(field.getValue0()));

    protected abstract <DS extends DataSchema> Collection<DataSchemaEntry<DS>> getDataSchemaEntries();

    public abstract List<Node> applyDataToTerms(Node term, Map<Variable, Type> variableTypes);

    public <T extends DataSchema> DataSchemaEntry<T> getParentEntry(){
        return null;
    }

    public <T extends DataSchema> void setParentEntry(DataSchemaEntry<T> parentEntry){
        // Do nothing by default. This is only required for schema that are used in the entry hierarchy like SimpleDataSchema.
    }

    public abstract void fillSchemaWithConstraints(Node term);

    public abstract <DS extends DataSchema, T> EvaluationResults<DS, T> evaluate(DataCollection<T> data);

    protected abstract <DS extends DataSchema> void collectAllConstraints(Map<DataSchemaEntry<DS>, Set<Constraint>> constraints, Map<DataSchemaEntry<DS>, Set<Constraint>> potentialConstraints);

    // TODO Think of a better name for this method.
    public <DS extends DataSchema, T> void applyConstraintRetentionPolicy(EvaluationResults<DS, T> evaluationResults, ConstraintPolicy... policies){
        applyConstraintRetentionPolicy(evaluationResults, new AndConstraintPolicy(policies));
    }

    public <DS extends DataSchema, T> void applyConstraintRetentionPolicy(EvaluationResults<DS, T> evaluationResults, ConstraintPolicy policy){
        evaluationResults.getPotentialConstraintResults().forEach((k, v) -> {
            for (ConstraintResults<T> constraintResults : v) {
                Constraint constraint = k.getPotentionConstraint(constraintResults.constraint());
                k.potentialConstraints.remove(constraint);
                if(policy.includeConstraint(constraintResults)){
                    k.constraints.add(constraint);
                }
            }
        });
    }

    /**
     *
     * @param terms - List of triplets linking nodes to the set of variables that still need to be set to a value and the nodes that have been inserted.
     * @param variableTypes - Map of variables that need to have a value assigned to them and their type.
     * @param schema
     */
    static <DS extends DataSchema> void findAssignableFields(List<Triplet<Node, Set<Variable>, Set<Node>>> terms, Map<Variable, Type> variableTypes, Collection<DataSchemaEntry<DS>> schema, FieldNodeProvider fieldNodeProvider){
        Set<Triplet<Node, Set<Variable>, Set<Node>>> internalReplacedTerms = new HashSet<>();
        Set<Map<Variable, Node>> variableAssignments = new HashSet<>();

        for (DataSchemaEntry<DS> schemaEntry : schema) {

            for (Map.Entry<Variable, Type> variableEntry : variableTypes.entrySet()) {
                Variable variable = variableEntry.getKey();
                Type variableType = variableEntry.getValue();

                if(!schemaEntry.type.canAssignTo(variableType)){
                    // TODO Here we could try to insert other node that extract properties of the fields, like the length of an array or a string.
                    continue;
                }

                Node value = fieldNodeProvider.generateNode(new Pair<>(schemaEntry.name, schemaEntry.type));
                Set<Map<Variable, Node>> variableAssignmentsToAdd = new HashSet<>();
                for (Map<Variable, Node> variableAssignment : variableAssignments) {
                    if(variableAssignment.containsKey(variable) || variableAssignment.containsValue(value)){
                        continue;
                    }

                    Map<Variable, Node> mapToFill = new HashMap<>(variableAssignment);
                    mapToFill.put(variable, value);
                    variableAssignmentsToAdd.add(mapToFill);
                }
                variableAssignments.addAll(variableAssignmentsToAdd);

                Map<Variable, Node> variableAssignment = new HashMap<>();
                variableAssignment.put(variable, value);
                variableAssignments.add(variableAssignment);
            }

            if(schemaEntry.type == TypeEnum.COMPLEXTYPE){
                List<Triplet<Node, Set<Variable>, Set<Node>>> internalTerms = new LinkedList<>(terms);
                findAssignableFields(internalTerms, variableTypes, schemaEntry.dataSchema.getDataSchemaEntries(), variableNodeProvider);
                for (Triplet<Node, Set<Variable>, Set<Node>> term : internalTerms) {
                    Map<Variable, Node> variableReplacements = new HashMap<>();
                    Set<Node> insertedNodes = new HashSet<>();
                    term.getValue0().visitNodes((VariableVisitor) variable -> {
                        if(term.getValue1().contains(variable)
                            || !term.getValue2().contains(variable)) {
                            return;
                        }

                        Variable insertedVariable = new Variable(schemaEntry.name + "." + variable.getName());
                        insertedNodes.add(insertedVariable);
                        variableReplacements.put(variable, insertedVariable);
                    });
                    Node replacementTerm = term.getValue0().setVariableValues(variableReplacements);
                    internalReplacedTerms.add(new Triplet<>(replacementTerm, term.getValue1(), insertedNodes));
                }
            } else if(schemaEntry.type instanceof ArrayType) {
                if (((ArrayType) schemaEntry.type).elementType() == TypeEnum.COMPLEXTYPE) {
                    List<Triplet<Node, Set<Variable>, Set<Node>>> internalTerms = new LinkedList<>(terms);
                    findAssignableFields(internalTerms, variableTypes, schemaEntry.dataSchema.getDataSchemaEntries(), arrayDereferenceProvider);
                    for (Triplet<Node, Set<Variable>, Set<Node>> term : internalTerms) {
                        Node value = fieldNodeProvider.generateNode(new Pair<>(schemaEntry.name, schemaEntry.type));
                        Node replacementTerm = new ForAll(value, term.getValue0());
                        Set<Node> insertedNodes = new HashSet<>();
                        insertedNodes.add(value);
                        internalReplacedTerms.add(new Triplet<>(replacementTerm, term.getValue1(), insertedNodes));
                    }
                } else if (((ArrayType) schemaEntry.type).elementType() instanceof ArrayType) {
                    List<Triplet<Node, Set<Variable>, Set<Node>>> internalTerms = new LinkedList<>(terms);
                    Collection<DataSchemaEntry<DS>> internalSchema = new LinkedList<>();
                    internalSchema.add(new DataSchemaEntry<>(null, schemaEntry.name, ((ArrayType) schemaEntry.type).elementType(), schemaEntry.mandatory, schemaEntry.dataSchema));
                    findAssignableFields(internalTerms, variableTypes, internalSchema, arrayElementProvider);
                    for (Triplet<Node, Set<Variable>, Set<Node>> term : internalTerms) {
                        Node value = fieldNodeProvider.generateNode(new Pair<>(schemaEntry.name, schemaEntry.type));
                        Node replacementTerm = new ForAll(value, term.getValue0());
                        Set<Node> insertedNodes = new HashSet<>();
                        insertedNodes.add(value);
                        internalReplacedTerms.add(new Triplet<>(replacementTerm, term.getValue1(), insertedNodes));
                    }
                }
            }
        }

        variableAssignments = new HashSet<>(variableAssignments);
        Set<Triplet<Node, Set<Variable>, Set<Node>>> replacedTerms = new HashSet<>();
        for (Map<Variable, Node> variableAssignment : variableAssignments) {
            for (Triplet<Node, Set<Variable>, Set<Node>> term : terms) {
                Node clonedTerm = term.getValue0().cloneNode();
                Node replacementTerm = clonedTerm.setVariableValues(variableAssignment);
                Set<Variable> replacedVariables = new HashSet<>(term.getValue1());
                replacedVariables.removeAll(variableAssignment.keySet());
                Set<Node> insertedNodes = new HashSet<>(term.getValue2());
                insertedNodes.addAll(variableAssignment.values());
                replacedTerms.add(new Triplet<>(replacementTerm, replacedVariables, insertedNodes));
            }

            for (Triplet<Node, Set<Variable>, Set<Node>> term : internalReplacedTerms) {
                Node clonedTerm = term.getValue0().cloneNode();
                Node replacementTerm = clonedTerm.setVariableValues(variableAssignment);
                Set<Variable> replacedVariables = new HashSet<>(term.getValue1());
                replacedVariables.removeAll(variableAssignment.keySet());
                Set<Node> insertedNodes = new HashSet<>(term.getValue2());
                insertedNodes.addAll(variableAssignment.values());
                replacedTerms.add(new Triplet<>(replacementTerm, replacedVariables, insertedNodes));
            }
        }
        if(variableAssignments.isEmpty()){
            replacedTerms.addAll(internalReplacedTerms);
        }

        terms.clear();
        terms.addAll(replacedTerms);
    }

    public interface DataSchemaEntrySelector<DS extends DataSchema>{
        DataSchemaEntry<DS> selectEntry(Collection<DataSchemaEntry<DS>> entries);
    }

    public static class HighestEntrySelector<DS extends DataSchema> implements DataSchemaEntrySelector<DS>{
        @Override
        public DataSchemaEntry<DS> selectEntry(Collection<DataSchemaEntry<DS>> entries) {
            return findHighestEntry(entries);
        }
    }

    public static class HighestEntryFromSchemaSelector<DS extends DataSchema> implements DataSchemaEntrySelector<DS>{
        private final DataSchema entryInSchema;
        public HighestEntryFromSchemaSelector(DataSchema entryInSchema) {
            this.entryInSchema = entryInSchema;
        }

        @Override
        public DataSchemaEntry<DS> selectEntry(Collection<DataSchemaEntry<DS>> entries) {
            return findHighestEntryInSchema(entries, entryInSchema);
        }
    }

    protected static <DS extends DataSchema> void fillSchemaWithConstraint(Node term, List<Map<Variable, DataSchemaEntry<DS>>> allCombinations, DataSchemaEntrySelector<DS> selector) {
        // For each combination, fill the term and attach it to the first DataSchemaEntry
        for (Map<Variable, DataSchemaEntry<DS>> combination : allCombinations) {
            Map<Variable, Node> replacement = new HashMap<>();
            combination.forEach((k, v) -> replacement.put(k, new DataReference(v)));

            Node filledTerm = term.setVariableValues(replacement);

            // Determine the first DataSchemaEntry used in the combination
            DataSchemaEntry<?> primaryEntry = selector.selectEntry(combination.values());

            // Add the filled term to the potentialConstraints of the primary entry
            primaryEntry.potentialConstraints.add(new Constraint(filledTerm));
        }
    }

    protected static <DS extends DataSchema> List<Map<Variable, DataSchemaEntry<DS>>> generateUniqueCombinations(Map<Variable, List<DataSchemaEntry<DS>>> matchingEntries) {
        List<Map<Variable, DataSchemaEntry<DS>>> results = new ArrayList<>();
        generateUniqueCombinationsRecursive(matchingEntries, new ArrayList<>(matchingEntries.keySet()), 0, new HashMap<>(), new HashSet<>(), results);
        return results;
    }

    private static <DS extends DataSchema> void generateUniqueCombinationsRecursive(Map<Variable, List<DataSchemaEntry<DS>>> matchingEntries,
                                                            List<Variable> placeholders, int index,
                                                            Map<Variable, DataSchemaEntry<DS>> currentCombination,
                                                            Set<DataSchemaEntry<DS>> usedEntries,
                                                            List<Map<Variable, DataSchemaEntry<DS>>> results) {
        if (index == placeholders.size()) {
            results.add(new HashMap<>(currentCombination));
            return;
        }

        Variable placeholder = placeholders.get(index);
        for (DataSchemaEntry<DS> entry : matchingEntries.get(placeholder)) {
            if (usedEntries.contains(entry)) continue;  // Skip if already used.

            currentCombination.put(placeholder, entry);
            usedEntries.add(entry);

            generateUniqueCombinationsRecursive(matchingEntries, placeholders, index + 1, currentCombination, usedEntries, results);

            currentCombination.remove(placeholder);
            usedEntries.remove(entry);  // Backtrack.
        }
    }

    protected static <DS extends DataSchema> List<Map<Variable, DataSchemaEntry<DS>>> generateValidCrossSchemaCombinations(
            Map<Variable, List<DataSchemaEntry<DS>>> entries1,
            Map<Variable, List<DataSchemaEntry<DS>>> entries2) {

        List<Map<Variable, DataSchemaEntry<DS>>> partialCombinations1 = generatePartialCombinations(entries1);
        List<Map<Variable, DataSchemaEntry<DS>>> partialCombinations2 = generatePartialCombinations(entries2);

        List<Map<Variable, DataSchemaEntry<DS>>> crossSchemaCombinations = new ArrayList<>();
        for (Map<Variable, DataSchemaEntry<DS>> combination1 : partialCombinations1) {
            for (Map<Variable, DataSchemaEntry<DS>> combination2 : partialCombinations2) {
                Map<Variable, DataSchemaEntry<DS>> merged = new HashMap<>(combination1);
                boolean hasOverlap = combination2.keySet().stream().anyMatch(merged::containsKey);

                if (!hasOverlap) {
                    merged.putAll(combination2);
                    if (!combination1.isEmpty() && !combination2.isEmpty()) {
                        crossSchemaCombinations.add(merged);
                    }
                }
            }
        }

        return crossSchemaCombinations;
    }

    public static <DS extends DataSchema> List<Map<Variable, DataSchemaEntry<DS>>> generatePartialCombinations(Map<Variable, List<DataSchemaEntry<DS>>> schemaEntries) {

        List<Map<Variable, DataSchemaEntry<DS>>> results = new ArrayList<>();
        List<Variable> placeholders = new ArrayList<>(schemaEntries.keySet());

        generatePartialCombinationsRecursive(new HashMap<>(), 0, placeholders, schemaEntries, results);

        return results;
    }

    private static <DS extends DataSchema> void generatePartialCombinationsRecursive(
            Map<Variable, DataSchemaEntry<DS>> currentCombination,
            int index,
            List<Variable> placeholders,
            Map<Variable, List<DataSchemaEntry<DS>>> schemaEntries,
            List<Map<Variable, DataSchemaEntry<DS>>> results) {

        if (index == placeholders.size()) {
            // Add the current partial combination, even if some placeholders are unfilled
            results.add(new HashMap<>(currentCombination));
            return;
        }

        Variable currentPlaceholder = placeholders.get(index);
        List<DataSchemaEntry<DS>> possibleEntries = schemaEntries.getOrDefault(currentPlaceholder, Collections.emptyList());

        // Try each possible entry for the current placeholder
        for (DataSchemaEntry<DS> entry : possibleEntries) {
            // Ensure the entry is not already used for another placeholder
            if (!currentCombination.containsValue(entry)) {
                currentCombination.put(currentPlaceholder, entry);
                generatePartialCombinationsRecursive(currentCombination, index + 1, placeholders, schemaEntries, results);
                currentCombination.remove(currentPlaceholder);  // Backtrack
            }
        }

        // Also consider the case where the current placeholder is left unfilled
        generatePartialCombinationsRecursive(currentCombination, index + 1, placeholders, schemaEntries, results);
    }

    private static <DS extends DataSchema> DataSchemaEntry<DS> findHighestEntry(Collection<DataSchemaEntry<DS>> entries) {
        if(entries == null || entries.isEmpty()){
            return null;
        }

        return entries.stream()
                .min(Comparator.comparingInt(DataSchema::getDepth))
                .orElse(null);
    }

    private static <DS extends DataSchema> DataSchemaEntry<DS> findHighestEntryInSchema(Collection<DataSchemaEntry<DS>> entries, DataSchema schema) {
        if(entries == null || entries.isEmpty()){
            return null;
        }

        return entries.stream()
                .filter(e -> isPartOfSchema(e, schema))
                .min(Comparator.comparingInt(DataSchema::getDepth))
                .orElse(null);
    }

    private static int getDepth(DataSchemaEntry<?> entry) {
        int depth = 0;
        if(entry == null){
            return depth;
        }

        entry = entry.getParentSchemaEntry();
        while (entry != null) {
            entry = entry.getParentSchemaEntry();
            depth++;
        }
        return depth;
    }

    private static boolean isPartOfSchema(DataSchemaEntry<?> entry, DataSchema schema) {
        if(entry == null || entry.parentSchema == null){
            return false;
        }
        if(entry.parentSchema == schema){
            return true;
        }

        return isPartOfSchema(entry.getParentSchemaEntry(), schema);
    }

    protected <DS extends DataSchema, T> void evaluateDataObject(
            Collection<DataSchemaEntry<DS>> schemaEntries,
            DataObject dao,
            T dataEntry,
            Map<Variable, Node> values,
            Map<DataSchemaEntry<DS>, Set<Constraint>> constraints,
            Map<DataSchemaEntry<DS>, Set<Constraint>> potentialConstraints,
            EvaluationResults<DS, T> evaluationResults
    ){
        for (DataSchemaEntry<DS> entry : schemaEntries) {
            DataValue<?> value = dao.getDataValue(entry.name);
            if(value == null){
                if(entry.mandatory){
                    evaluationResults.addResult(new MissingMandatoryValue<>(entry, dataEntry));
                }
                continue;
            }

            if(!value.getType().canAssignTo(entry.type)){
                evaluationResults.addResult(new IncompatibleTypes<>(entry, dataEntry, value.getType()));
            }

            if(entry.type == TypeEnum.COMPLEXTYPE){
                entry.dataSchema.evaluateDataObject(entry.dataSchema.getDataSchemaEntries(), (DataObject) value.getValue(), dataEntry, values, constraints, potentialConstraints, evaluationResults);
            } else {
                Value<?> literal = value.getLiteralValue();
                values.put(new DataReference(entry), literal);
            }

            constraints.put(entry, entry.constraints);
            potentialConstraints.put(entry, entry.potentialConstraints);
        }
    }
}
