package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.array.ArrayQuantifier;
import at.sfischer.constraints.model.operators.array.ForAll;
import at.sfischer.constraints.model.operators.objects.Reference;
import org.javatuples.Pair;

import java.util.*;

public abstract class DataCollection<T> {

    public DataSchema deriveSchema(){
        return deriveSchema(null);
    }

    public abstract DataSchema deriveSchema(TypePromotionPolicy typePromotionPolicy);

    public abstract boolean applyDataToTerms(List<Node> terms, Map<Variable, Type> variableTypes);

    public abstract int numberOfDataEntries();

    public void visitDataEntries(DataEntryVisitor<T> visitor){
        visitDataEntries(new HashSet<>(), visitor);
    }

    public abstract void visitDataEntries(Set<String> fieldNames, DataEntryVisitor<T> visitor);

    public void addAll(DataCollection<T> collection){
        collection.visitDataEntries(new HashSet<>(), (values, dataEntry) -> addDataEntry(dataEntry));
    }

    public abstract void addDataEntry(T dataEntry);

    public abstract void removeDataEntry(T dataEntry);

    public abstract void clear();

    public abstract DataCollection<T> emptyDataCollection();

    public abstract DataCollection<T> clone();

    public abstract int size();

    public abstract List<List<Value<?>>> getAllValues(String valueReference);

    public abstract List<Map<Variable, Node>> getAllValueCombinations(Set<Variable> variables);

    protected interface FieldNodeProvider{
        Node generateNode(Pair<String, DataValue<?>> field);
    }

    protected static final FieldNodeProvider variableNodeProvider = field -> new Variable(field.getValue0());
    protected static final FieldNodeProvider arrayElementProvider = field -> new Variable(ArrayQuantifier.ELEMENT_NAME);
    protected static final FieldNodeProvider arrayDereferenceProvider = field -> new Reference(new Variable(ArrayQuantifier.ELEMENT_NAME), new StringLiteral(field.getValue0()));

    protected static void findAssignableFields(List<Pair<Node, Set<Variable>>> terms, Map<Variable, Type> variableTypes, FieldNodeProvider fieldNodeProvider, DataObject[] dataCollection){
        findAssignableFields(terms, variableTypes, fieldNodeProvider, Arrays.asList(dataCollection));
    }

    protected static void findAssignableFields(List<Pair<Node, Set<Variable>>> terms, Map<Variable, Type> variableTypes, FieldNodeProvider fieldNodeProvider, Collection<DataObject> dataCollection){
        Set<Pair<Node, Set<Variable>>> internalReplacedTerms = new HashSet<>();
        Set<Map<Variable, Node>> variableAssignments = new HashSet<>();
        for (DataObject dataObject : dataCollection) {
            Map<Type, List<Pair<String, DataValue<?>>>> dataTypes = dataObject.getValuesByType();
            for (Map.Entry<Type, List<Pair<String, DataValue<?>>>> entry : dataTypes.entrySet()) {
                Type type = entry.getKey();
                List<Pair<String, DataValue<?>>> fields = entry.getValue();

                for (Map.Entry<Variable, Type> variableEntry : variableTypes.entrySet()) {
                    Variable variable = variableEntry.getKey();
                    Type variableType = variableEntry.getValue();
                    if(!type.canAssignTo(variableType)){
                        // TODO Here we could try to insert other node that extract properties of the fields, like the length of an array or a string.
                        continue;
                    }

                    for (Pair<String, DataValue<?>> field : fields) {
                        Node value = fieldNodeProvider.generateNode(field);

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
                }

                if(type instanceof ArrayType) {
                    if (((ArrayType) type).elementType() == TypeEnum.COMPLEXTYPE) {
                        for (Pair<String, DataValue<?>> field : fields) {
                            DataValue<?> dataValue = field.getValue1();
                            if (dataValue.getValue() instanceof DataObject[]) {
                                Node value = fieldNodeProvider.generateNode(field);
                                List<Pair<Node, Set<Variable>>> internalTerms = new LinkedList<>(terms);
                                findAssignableFields(internalTerms, variableTypes, arrayDereferenceProvider, (DataObject[]) dataValue.getValue());
                                // Transform term by inserting it into a ForAll node use the current field value as the array parameter.
                                for (Pair<Node, Set<Variable>> term : internalTerms) {
                                    Node replacementTerm = new ForAll(value, term.getValue0());
                                    internalReplacedTerms.add(new Pair<>(replacementTerm, term.getValue1()));
                                }
                            }
                        }
                    } else if (((ArrayType) type).elementType() instanceof ArrayType) {
                        for (Pair<String, DataValue<?>> field : fields) {
                            DataValue<?> dataValue = field.getValue1();
                            if (dataValue.getValue() instanceof DataValue[]) {
                                Node value = fieldNodeProvider.generateNode(field);
                                for (DataValue<?> dataValueArray : (DataValue<?>[]) dataValue.getValue()) {
                                    DataObject dao = new DataObject();
                                    dao.putDataValue(field.getValue0(), dataValueArray);
                                    List<Pair<Node, Set<Variable>>> internalTerms = new LinkedList<>(terms);
                                    findAssignableFields(internalTerms, variableTypes, arrayElementProvider, new DataObject[]{dao});
                                    // Transform term by inserting it into a ForAll node use the current field value as the array parameter.
                                    for (Pair<Node, Set<Variable>> term : internalTerms) {
                                        Node replacementTerm = new ForAll(value, term.getValue0());
                                        internalReplacedTerms.add(new Pair<>(replacementTerm, term.getValue1()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        variableAssignments = new HashSet<>(variableAssignments);
        Set<Pair<Node, Set<Variable>>> replacedTerms = new HashSet<>();
        for (Map<Variable, Node> variableAssignment : variableAssignments) {
            for (Pair<Node, Set<Variable>> term : terms) {
                Node clonedTerm = term.getValue0().cloneNode();
                Node replacementTerm = clonedTerm.setVariableValues(variableAssignment);
                Set<Variable> replacedVariables = new HashSet<>(term.getValue1());
                replacedVariables.addAll(variableAssignment.keySet());
                replacedTerms.add(new Pair<>(replacementTerm, replacedVariables));
            }

            for (Pair<Node, Set<Variable>> term : internalReplacedTerms) {
                Node clonedTerm = term.getValue0().cloneNode();
                Node replacementTerm = clonedTerm.setVariableValues(variableAssignment);
                Set<Variable> replacedVariables = new HashSet<>(term.getValue1());
                replacedVariables.addAll(variableAssignment.keySet());
                replacedTerms.add(new Pair<>(replacementTerm, replacedVariables));
            }
        }
        if(variableAssignments.isEmpty()){
            replacedTerms.addAll(internalReplacedTerms);
        }

        terms.clear();
        terms.addAll(replacedTerms);
    }
}
