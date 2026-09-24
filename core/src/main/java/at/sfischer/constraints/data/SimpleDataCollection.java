package at.sfischer.constraints.data;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.Value;
import at.sfischer.constraints.model.Variable;
import org.javatuples.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class SimpleDataCollection extends DataCollection<DataObject> {

    private final List<DataObject> dataCollection;

    public SimpleDataCollection() {
        this.dataCollection = new LinkedList<>();
    }

    @Override
    public SimpleDataSchema deriveSchema(TypePromotionPolicy typePromotionPolicy) {
        SimpleDataSchema schema = null;
        for (DataObject dataObject : dataCollection) {
            if(schema == null){
                schema = SimpleDataSchema.deriveFromData(dataObject);
            } else {
                schema.unify(SimpleDataSchema.deriveFromData(dataObject), typePromotionPolicy);
            }
        }

        return schema;
    }

    public List<DataObject> getDataCollection() {
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
    public void clear() {
        dataCollection.clear();
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

    @Override
    public int size() {
        return this.dataCollection.size();
    }

    @Override
    public List<List<Value<?>>> getAllValues(String valueReference) {
        List<List<Value<?>>> allValues = new LinkedList<>();
        for (DataObject dataObject : getDataCollection()) {
            List<Value<?>> values = dataObject.getValues(valueReference);
            if(values == null){
                continue;
            }

            allValues.add(values);
        }

        return allValues;
    }

    @Override
    public List<Map<Variable, Node>> getAllValueCombinations(Set<Variable> variables) {
        List<Map<Variable, Node>> valueCombinations = new LinkedList<>();
        Map<Utils.Path, Utils.Group> groups = Utils.groupByCollapsedHierarchy(variables);
        for (DataObject dataObject : getDataCollection()) {
            List<Map<Variable, Node>> dataValues = Utils.collectValueCombinations(dataObject, groups);
            valueCombinations.addAll(dataValues);
        }
        return valueCombinations;
    }

    protected Map<String, Type> getDataTypes(){
        return getDataTypes(this.dataCollection);
    }

    protected Map<String, Type> getDataTypes(Collection<DataObject> dataCollection){
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

    private void findAssignableFields(List<Pair<Node, Set<Variable>>> terms, Map<Variable, Type> variableTypes){
        findAssignableFields(terms, variableTypes, variableNodeProvider, this.dataCollection);
    }

    @Override
    public boolean applyDataToTerms(List<Node> terms, Map<Variable, Type> variableTypes){
        List<Pair<Node, Set<Variable>>> termsToAssign = new LinkedList<>();
        terms.forEach(term -> termsToAssign.add(new Pair<>(term, new HashSet<>())));
        findAssignableFields(termsToAssign, variableTypes);

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
    public DataCollection<DataObject> emptyDataCollection() {
        return new SimpleDataCollection();
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

    public static SimpleDataCollection parseData(File jsonl) throws IOException {
        SimpleDataCollection dataCollection = new SimpleDataCollection();
        try (BufferedReader reader = Files.newBufferedReader(
                jsonl.toPath(),
                StandardCharsets.UTF_8)) {

            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    dataCollection.addDataEntry(DataObject.parseData(line));
                } catch (RuntimeException e) {
                    throw new IOException("Could not parse JSONL line " + lineNumber + " in file " + jsonl, e);
                }
            }
        }

        return dataCollection;
    }

    @Override
    public void toJsonl(File jsonl) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                jsonl.toPath(),
                StandardCharsets.UTF_8)) {

            for (DataObject data : dataCollection) {
                writer.write(data.toJson());
                writer.newLine();
            }
        }
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
