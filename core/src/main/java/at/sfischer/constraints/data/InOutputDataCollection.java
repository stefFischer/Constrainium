package at.sfischer.constraints.data;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.Value;
import at.sfischer.constraints.model.Variable;
import org.javatuples.Pair;

import java.util.*;

public class InOutputDataCollection extends DataCollection<Pair<DataObject, DataObject>> {

    private final List<Pair<DataObject, DataObject>> dataCollection;

    public InOutputDataCollection() {
        this.dataCollection = new LinkedList<>();
    }

    public List<Pair<DataObject, DataObject>> getDataCollection() {
        return dataCollection;
    }

    @Override
    public InOutputDataSchema<SimpleDataSchema> deriveSchema(TypePromotionPolicy typePromotionPolicy) {
        SimpleDataSchema inputSchema = null;
        SimpleDataSchema outputSchema = null;
        for (Pair<DataObject, DataObject> pair : dataCollection) {
            if(inputSchema == null){
                inputSchema = SimpleDataSchema.deriveFromData(pair.getValue0());
            } else {
                inputSchema.unify(SimpleDataSchema.deriveFromData(pair.getValue0()), typePromotionPolicy);
            }
            if(outputSchema == null){
                outputSchema = SimpleDataSchema.deriveFromData(pair.getValue1());
            } else {
                outputSchema.unify(SimpleDataSchema.deriveFromData(pair.getValue1()), typePromotionPolicy);
            }
        }

        return new InOutputDataSchema<>(inputSchema, outputSchema);
    }

    @Override
    public int size() {
        return this.dataCollection.size();
    }

    @Override
    public List<List<Value<?>>> getAllValues(String valueReference) {
        List<List<Value<?>>> allValues = new LinkedList<>();
        for (Pair<DataObject, DataObject> pair : getDataCollection()) {
            List<Value<?>> values = pair.getValue0().getValues(valueReference);
            if(values == null){
                values = pair.getValue1().getValues(valueReference);
                if(values == null){
                    continue;
                }
            }

            allValues.add(values);
        }

        return allValues;
    }

    @Override
    public List<Map<Variable, Node>> getAllValueCombinations(Set<Variable> variables) {
        List<Map<Variable, Node>> valueCombinations = new LinkedList<>();
        for (Pair<DataObject, DataObject> pair : getDataCollection()) {
            List<Map<Variable, Node>> dataValues0 = Utils.collectValueCombinations(pair.getValue0(), variables);
            List<Map<Variable, Node>> dataValues1 = Utils.collectValueCombinations(pair.getValue1(), variables);
            Utils.addValueCombinations(dataValues0, dataValues1);
            valueCombinations.addAll(dataValues0);
        }
        return valueCombinations;
    }

    private void findAssignableInputFields(List<Pair<Node, Set<Variable>>> terms, Map<Variable, Type> variableTypes){
        Set<DataObject> inputDataCollection = new HashSet<>();
        this.dataCollection.forEach(data -> inputDataCollection.add(data.getValue0()));
        findAssignableFields(terms, variableTypes, variableNodeProvider, inputDataCollection);
    }

    private void findAssignableOutputFields(List<Pair<Node, Set<Variable>>> terms, Map<Variable, Type> variableTypes){
        Set<DataObject> outputDataCollection = new HashSet<>();
        this.dataCollection.forEach(data -> outputDataCollection.add(data.getValue1()));
        findAssignableFields(terms, variableTypes, variableNodeProvider, outputDataCollection);
    }

    @Override
    public boolean applyDataToTerms(List<Node> terms, Map<Variable, Type> variableTypes) {
        List<Pair<Node, Set<Variable>>> termsToAssign = new LinkedList<>();
        terms.forEach(term -> termsToAssign.add(new Pair<>(term, new HashSet<>())));
        findAssignableInputFields(termsToAssign, variableTypes);
        termsToAssign.removeIf(pair -> pair.getValue1().containsAll(variableTypes.keySet()));

        findAssignableOutputFields(termsToAssign, variableTypes);

        terms.clear();
        for (Pair<Node, Set<Variable>> pair : termsToAssign) {
            // Check if all variables have been assigned a field.
            if(pair.getValue1().containsAll(variableTypes.keySet())){
                terms.add(pair.getValue0());
            }
        }

        return !terms.isEmpty();
    }

    @Override
    public int numberOfDataEntries() {
        return this.dataCollection.size();
    }

    @Override
    public void visitDataEntries(Set<String> fieldNames, DataEntryVisitor<Pair<DataObject, DataObject>> visitor) {
        for (Pair<DataObject, DataObject> dataObjectPair : dataCollection) {
            Map<String, Node> inputValues = dataObjectPair.getValue0().getDataValues();
            for (String fieldName : new HashSet<>(inputValues.keySet())) {
                if(!fieldNames.contains(fieldName)){
                    inputValues.remove(fieldName);
                }
            }
            Map<String, Node> outputValues = dataObjectPair.getValue1().getDataValues();
            for (String fieldName : new HashSet<>(outputValues.keySet())) {
                if(!fieldNames.contains(fieldName)){
                    outputValues.remove(fieldName);
                }
            }
            inputValues.putAll(outputValues);
            visitor.visitDataValues(inputValues, dataObjectPair);
        }
    }

    @Override
    public void addDataEntry(Pair<DataObject, DataObject> dataEntry) {
        dataCollection.add(dataEntry);
    }

    @Override
    public void removeDataEntry(Pair<DataObject, DataObject> dataEntry) {
        dataCollection.remove(dataEntry);
    }

    @Override
    public void clear() {
        dataCollection.clear();
    }

    @Override
    public DataCollection<Pair<DataObject, DataObject>> emptyDataCollection() {
        return new InOutputDataCollection();
    }

    @Override
    public DataCollection<Pair<DataObject, DataObject>> clone() {
        InOutputDataCollection clone = new InOutputDataCollection();
        clone.dataCollection.addAll(this.dataCollection);
        return clone;
    }

    public static InOutputDataCollection parseData(Pair<String, String>... data){
        return parseData(Arrays.asList(data));
    }

    public static InOutputDataCollection parseData(Collection<Pair<String, String>> data){
        InOutputDataCollection dataCollection = new InOutputDataCollection();
        for (Pair<String, String> datum : data) {
            DataObject inputDao = DataObject.parseData(datum.getValue0());
            DataObject outputDao = DataObject.parseData(datum.getValue1());
            dataCollection.addDataEntry(new Pair<>(inputDao, outputDao));
        }

        return dataCollection;
    }
}
