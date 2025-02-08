package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
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

    protected abstract Collection<DataSchemaEntry<?>> getDataSchemaEntries();

    public abstract List<Node> applyDataToTerms(Node term, Map<Variable, Type> variableTypes);

    public abstract <T extends DataSchema> DataSchemaEntry<T> getParentEntry();

    public abstract <T extends DataSchema> void setParentEntry(DataSchemaEntry<T> parentEntry);

    public abstract <DS extends DataSchema, T> EvaluationResults<DS, T> evaluate(DataCollection<T> data);

    /**
     *
     * @param terms - List of triplets linking nodes to the set of variables that still need to be set to a value and the nodes that have been inserted.
     * @param variableTypes - Map of variables that need to have a value assigned to them and their type.
     * @param schema
     */
    static void findAssignableFields(List<Triplet<Node, Set<Variable>, Set<Node>>> terms, Map<Variable, Type> variableTypes, Collection<DataSchemaEntry<?>> schema, FieldNodeProvider fieldNodeProvider){
        Set<Triplet<Node, Set<Variable>, Set<Node>>> internalReplacedTerms = new HashSet<>();
        Set<Map<Variable, Node>> variableAssignments = new HashSet<>();

        for (DataSchemaEntry<?> schemaEntry : schema) {

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
                    Collection<DataSchemaEntry<?>> internalSchema = new LinkedList<>();
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

    public static void fillSchemaWithConstraints(Node term, Collection<DataSchemaEntry<?>> schema) {
        fillSchemaWithConstraints(term, schema, 0);
    }

    private static void fillSchemaWithConstraints(Node term, Collection<DataSchemaEntry<?>> schema, int recursiveCount) {
        Map<Variable, Type> placeholderTypes = term.inferVariableTypes();
        Map<Variable, List<DataSchemaEntry<?>>> matchingEntries = new HashMap<>();

        // Find all matching schema entries for each placeholder
        for (Map.Entry<Variable, Type> placeholder : placeholderTypes.entrySet()) {
            List<DataSchemaEntry<?>> matches = findMatchingEntries(schema, placeholder.getKey(), placeholder.getValue(), term, recursiveCount);
            matchingEntries.put(placeholder.getKey(), matches);
        }

        // TODO For combinations of in and output I will need to call this multiple times and allow here to make all possible sub assignments.
        //  But I need to do this before inserting the constraints into schema entry.
        // Generate all possible combinations of schema entries that fit the placeholders
        List<Map<Variable, DataSchemaEntry<?>>> allCombinations = generateUniqueCombinations(matchingEntries);

        // For each combination, fill the term and attach it to the first DataSchemaEntry
        for (Map<Variable, DataSchemaEntry<?>> combination : allCombinations) {
            Map<Variable, Node> replacement = new HashMap<>();
            combination.forEach((k, v) -> replacement.put(k, new DataReference(v)));

            Node filledTerm = term.setVariableValues(replacement);

            // Determine the first DataSchemaEntry used in the combination
            DataSchemaEntry<?> primaryEntry = findHighestEntry(combination.values());

            // Add the filled term to the potentialConstraints of the primary entry
            primaryEntry.potentialConstraints.add(new Constraint(filledTerm));
        }
    }

    private static List<DataSchemaEntry<?>> findMatchingEntries(Collection<DataSchemaEntry<?>> schema, Variable variable, Type valueType, Node term, int recursiveCount) {
        List<DataSchemaEntry<?>> matches = new ArrayList<>();
        for (DataSchemaEntry<?> entry : schema) {
            if (valueType.canAssignTo(entry.type)) {
                matches.add(entry);
            } else if(entry.type instanceof ArrayType){
                if (valueType.canAssignTo(((ArrayType) entry.type).elementType())) {
                    if(recursiveCount <= 0) {
                        Node replacedTerm = new ForAll(variable, term.setVariableValue(variable, new Variable(ArrayQuantifier.ELEMENT_NAME)));
                        fillSchemaWithConstraints(replacedTerm, schema, recursiveCount + 1);
                    }
                    continue;
                } else if(((ArrayType) entry.type).elementType() == TypeEnum.COMPLEXTYPE){
                    if(recursiveCount <= 0) {
                        // Find values inside complex value that can be inserted into the term.
                        List<DataSchemaEntry<?>> innerMatches = findMatchingEntries(entry.dataSchema.getDataSchemaEntries(), variable, valueType, term, recursiveCount);
                        for (DataSchemaEntry<?> innerMatch : innerMatches) {
                            Node replacedTerm = new ForAll(variable, term.setVariableValue(variable, new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral(innerMatch.getQualifiedName().substring(entry.getQualifiedName().length() + 1)))));
                            fillSchemaWithConstraints(replacedTerm, schema, recursiveCount + 1);
                        }
                    }
                    continue;
                }
            }
            if (entry.dataSchema != null) {
                matches.addAll(findMatchingEntries(entry.dataSchema.getDataSchemaEntries(), variable, valueType, term, recursiveCount));
            }
        }

        return matches;
    }

    private static List<Map<Variable, DataSchemaEntry<?>>> generateUniqueCombinations(Map<Variable, List<DataSchemaEntry<?>>> matchingEntries) {
        List<Map<Variable, DataSchemaEntry<?>>> results = new ArrayList<>();
        generateUniqueCombinationsRecursive(matchingEntries, new ArrayList<>(matchingEntries.keySet()), 0, new HashMap<>(), new HashSet<>(), results);
        return results;
    }

    private static void generateUniqueCombinationsRecursive(Map<Variable, List<DataSchemaEntry<?>>> matchingEntries,
                                                            List<Variable> placeholders, int index,
                                                            Map<Variable, DataSchemaEntry<?>> currentCombination,
                                                            Set<DataSchemaEntry<?>> usedEntries,
                                                            List<Map<Variable, DataSchemaEntry<?>>> results) {
        if (index == placeholders.size()) {
            results.add(new HashMap<>(currentCombination));
            return;
        }

        Variable placeholder = placeholders.get(index);
        for (DataSchemaEntry<?> entry : matchingEntries.get(placeholder)) {
            if (usedEntries.contains(entry)) continue;  // Skip if already used.

            currentCombination.put(placeholder, entry);
            usedEntries.add(entry);

            generateUniqueCombinationsRecursive(matchingEntries, placeholders, index + 1, currentCombination, usedEntries, results);

            currentCombination.remove(placeholder);
            usedEntries.remove(entry);  // Backtrack.
        }
    }

    public static DataSchemaEntry<?> findHighestEntry(Collection<DataSchemaEntry<?>> entries) {
        if(entries == null || entries.isEmpty()){
            return null;
        }

        return entries.stream()
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
}
