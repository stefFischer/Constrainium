package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;

import java.util.*;

public class SimpleDataCollection implements DataCollection<DataObject> {

    private final Set<DataObject> dataCollection;

    public SimpleDataCollection() {
        this.dataCollection = new HashSet<>();
    }

    public Set<DataObject> getDataCollection() {
        return dataCollection;
    }

    @Override
    public void addDataEntry(DataObject dataObject){
        dataCollection.add(dataObject);
    }

    @Override
    public void removeDataEntry(DataObject dataObject) {
        dataCollection.remove(dataObject);
    }

    @Override
    public int numberOfDataEntries(){
        return this.dataCollection.size();
    }

    @Override
    public void visitDataEntries(Set<String> fieldNames, DataEntryVisitor<DataObject> visitor) {
        for (DataObject dataObject : getDataCollection()) {
            Map<String, Node> values = dataObject.getDataValues();
            for (String fieldName : new HashSet<>(values.keySet())) {
                if(!fieldNames.contains(fieldName)){
                    values.remove(fieldName);
                }
            }
            visitor.visitDataValues(values, dataObject);
        }
    }

    protected Map<String, Type> getDataTypes(){
        Map<String, Type> dataTypes = new HashMap<>();
        for (DataObject dataObject : dataCollection) {
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

    protected Map<Type, List<String>> getFieldTypes(){
        Map<Type, List<String>> fieldTypes = new HashMap<>();
        Map<String, Type> dataTypes = getDataTypes();
        for (Map.Entry<String, Type> entry : dataTypes.entrySet()) {
            List<String> dataForType = fieldTypes.computeIfAbsent(entry.getValue(), k -> new ArrayList<>());
            dataForType.add(entry.getKey());
        }

        return fieldTypes;
    }

    @Override
    public boolean applyDataToTerms(List<Node> terms, Map<Variable, Type> variableTypes){
        Map<Type, List<String>> fieldTypes = getFieldTypes();
        for (Map.Entry<Variable, Type> entry : variableTypes.entrySet()) {
            List<String> dataForType = getAssignableFields(fieldTypes, entry.getValue());
            if(dataForType.isEmpty()){
                return false;
            }

            List<Node> nextReplacedTerms = new LinkedList<>();
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

        return true;
    }

    private List<String> getAssignableFields(Map<Type, List<String>> fieldTypes, Type variableType){
        List<String> fields = new LinkedList<>();
        for (Map.Entry<Type, List<String>> entry : fieldTypes.entrySet()) {
            if(entry.getKey().canAssignTo(variableType)){
                fields.addAll(entry.getValue());
            }
        }

        return fields;
    }

    @Override
    public DataCollection<DataObject> clone() {
        SimpleDataCollection clone = new SimpleDataCollection();
        clone.dataCollection.addAll(this.dataCollection);
        return clone;
    }

    public static SimpleDataCollection parseData(String... data){
        return parseData(Arrays.asList(data));
    }

    public static SimpleDataCollection parseData(Collection<String> data){
        SimpleDataCollection dataCollection = new SimpleDataCollection();
        for (String datum : data) {
            DataObject dao = DataObject.parseData(datum);
            dataCollection.addDataEntry(dao);
        }

        return dataCollection;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("SimpleDataCollection {\n\t");
        s.append("dataCollection=[");
        for (DataObject dataObject : dataCollection) {
            s.append("\n\t\t");
            s.append(dataObject);
        }
        s.append("\n\t]");
        s.append("\n}");
        return s.toString();
    }
}
