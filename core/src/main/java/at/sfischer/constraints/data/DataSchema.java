package at.sfischer.constraints.data;

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
                    internalSchema.add(new DataSchemaEntry<>(schemaEntry.name, ((ArrayType) schemaEntry.type).elementType(), schemaEntry.mandatory, schemaEntry.dataSchema));
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
                Node replacementTerm = term.getValue0().setVariableValues(variableAssignment);
                Set<Variable> replacedVariables = new HashSet<>(term.getValue1());
                replacedVariables.removeAll(variableAssignment.keySet());
                Set<Node> insertedNodes = new HashSet<>(term.getValue2());
                insertedNodes.addAll(variableAssignment.values());
                replacedTerms.add(new Triplet<>(replacementTerm, replacedVariables, insertedNodes));
            }

            for (Triplet<Node, Set<Variable>, Set<Node>> term : internalReplacedTerms) {
                Node replacementTerm = term.getValue0().setVariableValues(variableAssignment);
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

    public static class DataSchemaEntry<T extends DataSchema> {
        public final String name;

        public final Type type;

        public final boolean mandatory;

        public final T dataSchema;

        public DataSchemaEntry(String name, Type type, boolean mandatory, T dataSchema) {
            this.name = name;
            this.type = type;
            this.mandatory = mandatory;
            this.dataSchema = dataSchema;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataSchemaEntry<?> that = (DataSchemaEntry<?>) o;
            return mandatory == that.mandatory && Objects.equals(name, that.name) && Objects.equals(type, that.type) && Objects.equals(dataSchema, that.dataSchema);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, mandatory, dataSchema);
        }

        @Override
        public String toString() {
            return name + ": " + type + " (" + (mandatory ? "mandatory" : "optional") + ")";
        }
    }
}
