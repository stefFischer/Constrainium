package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;
import org.javatuples.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class InOutputDataCollection implements DataCollection<Pair<DataObject, DataObject>> {

    private final Set<Pair<DataObject, DataObject>> dataCollection;

    public InOutputDataCollection() {
        this.dataCollection = new HashSet<>();
    }

    protected Map<String, Type> getDataTypes(DataObjectRetriever retriever){
        Map<String, Type> dataTypes = new HashMap<>();
        for (Pair<DataObject, DataObject> pair : dataCollection) {
            DataObject dataObject = retriever.getDataObject(pair);
            Map<String, Type> dataObjectTypes = dataObject.getDataTypes();
            for (Map.Entry<String, Type> typeEntry : dataObjectTypes.entrySet()) {
                Type t = dataTypes.get(typeEntry.getKey());
                if(t != null && !t.equals(typeEntry.getValue())){
                    throw new IllegalStateException("Field " + typeEntry.getKey() + " has inconsistent types: " + t + " and " + typeEntry.getValue());
                }

                if(t == null){
                    dataTypes.put(typeEntry.getKey(), typeEntry.getValue());
                }
            }
        }

        return dataTypes;
    }

    protected Map<Type, List<String>> getFieldTypes(DataObjectRetriever retriever){
        Map<Type, List<String>> fieldTypes = new HashMap<>();
        Map<String, Type> dataTypes = getDataTypes(retriever);
        for (Map.Entry<String, Type> entry : dataTypes.entrySet()) {
            List<String> dataForType = fieldTypes.computeIfAbsent(entry.getValue(), k -> new LinkedList<>());
            dataForType.add(entry.getKey());
        }

        return fieldTypes;
    }

    @Override
    public boolean applyDataToTerms(List<Node> terms, Map<Variable, Type> variableTypes) {
        if(variableTypes.size() < 2){
            return false;
        }

        Map<Type, List<String>> inputFieldTypes = getFieldTypes(new InputDataRetriever());
        Map<Type, List<String>> outputFieldTypes = getFieldTypes(new OutputDataRetriever());
        applyDataToOneVariable(terms, variableTypes, inputFieldTypes);
        applyDataToOneVariable(terms, variableTypes, outputFieldTypes);

        for (Map.Entry<Type, List<String>> entry : inputFieldTypes.entrySet()) {
            List<String> fields = outputFieldTypes.computeIfAbsent(entry.getKey(), k -> new LinkedList<>());
            fields.addAll(entry.getValue());
        }
        applyDataToAllVariable(terms, variableTypes, outputFieldTypes);

        return true;
    }

    private void applyDataToOneVariable(List<Node> terms, Map<Variable, Type> variableTypes, Map<Type, List<String>> fieldTypes){
        List<Node> nextReplacedTerms = new LinkedList<>();
        for (Map.Entry<Variable, Type> entry : variableTypes.entrySet()) {
            List<String> dataForType = fieldTypes.get(entry.getValue());
            if(dataForType == null){
                continue;
            }

            for (String name : dataForType) {
                for (Node term : terms) {
                    Node clone = term.setVariableValues(new HashMap<>());
                    AtomicBoolean replaced = new AtomicBoolean(false);
                    clone.visitNodes((VariableVisitor) variable -> {
                        if(!variable.equals(entry.getKey())){
                            return;
                        }

                        variable.setReplacementName(name);
                        replaced.set(true);
                    });

                    if(replaced.get()){
                        nextReplacedTerms.add(clone);
                    }
                }
            }
        }

        terms.clear();
        terms.addAll(nextReplacedTerms);
        for (Node replacedTerm : terms) {
            replacedTerm.visitNodes((VariableVisitor) Variable::replaceName);
        }
    }

    private void applyDataToAllVariable(List<Node> terms, Map<Variable, Type> variableTypes, Map<Type, List<String>> fieldTypes){
        for (Map.Entry<Variable, Type> entry : variableTypes.entrySet()) {
            List<Node> nextReplacedTerms = new LinkedList<>();
            List<String> dataForType = fieldTypes.get(entry.getValue());
            if(dataForType == null){
                continue;
            }

            for (String name : dataForType) {
                for (Node term : terms) {
                    Node clone = term.setVariableValues(new HashMap<>());
                    clone.visitNodes((VariableVisitor) variable -> {
                        if(!variable.equals(entry.getKey())){
                            return;
                        }

                        variable.setReplacementName(name);
                    });

                    nextReplacedTerms.add(clone);
                }
            }

            terms.clear();
            terms.addAll(nextReplacedTerms);
            for (Node replacedTerm : terms) {
                replacedTerm.visitNodes((VariableVisitor) Variable::replaceName);
            }
        }
    }

    @Override
    public int numberOfDataEntries() {
        return this.dataCollection.size();
    }

    @Override
    public void visitDataEntries(Set<String> fieldNames, DataEntryVisitor<Pair<DataObject, DataObject>> visitor) {
        for (Pair<DataObject, DataObject> dataObjectPair : dataCollection) {
            Map<String, Literal<?>> inputValues = dataObjectPair.getValue0().getDataValues();
            for (String fieldName : new HashSet<>(inputValues.keySet())) {
                if(!fieldNames.contains(fieldName)){
                    inputValues.remove(fieldName);
                }
            }
            Map<String, Literal<?>> outputValues = dataObjectPair.getValue1().getDataValues();
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

    protected interface DataObjectRetriever {
        DataObject getDataObject(Pair<DataObject, DataObject> pair);
    }

    protected static class InputDataRetriever implements DataObjectRetriever{
        @Override
        public DataObject getDataObject(Pair<DataObject, DataObject> pair) {
            return pair.getValue0();
        }
    }

    protected static class OutputDataRetriever implements DataObjectRetriever{
        @Override
        public DataObject getDataObject(Pair<DataObject, DataObject> pair) {
            return pair.getValue1();
        }
    }
}
