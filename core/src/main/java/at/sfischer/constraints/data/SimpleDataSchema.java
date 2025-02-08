package at.sfischer.constraints.data;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.ConstraintResults;
import at.sfischer.constraints.model.*;
import org.javatuples.Triplet;

import java.util.*;

public class SimpleDataSchema extends DataSchema {

    private DataSchemaEntry<SimpleDataSchema> parentEntry = null;

    private final Map<String, DataSchemaEntry<SimpleDataSchema>> schema;

    @Override
    protected Collection<DataSchemaEntry<?>> getDataSchemaEntries() {
        return new HashSet<>(schema.values());
    }

    public SimpleDataSchema() {
        this.schema = new HashMap<>();
    }

    public DataSchemaEntry<SimpleDataSchema> booleanEntry(String name, boolean mandatory){
        return schema.computeIfAbsent(name, k -> new DataSchemaEntry<>(this, name, TypeEnum.BOOLEAN, mandatory, null));
    }

    public DataSchemaEntry<SimpleDataSchema> numberEntry(String name, boolean mandatory){
        return schema.computeIfAbsent(name, k -> new DataSchemaEntry<>(this, name, TypeEnum.NUMBER, mandatory, null));
    }

    public DataSchemaEntry<SimpleDataSchema> stringEntry(String name, boolean mandatory){
        return schema.computeIfAbsent(name, k -> new DataSchemaEntry<>(this, name, TypeEnum.STRING, mandatory, null));
    }

    public DataSchemaEntry<SimpleDataSchema> objectEntry(String name, boolean mandatory){
        return schema.computeIfAbsent(name, k -> new DataSchemaEntry<>(this, name, TypeEnum.COMPLEXTYPE, mandatory, new SimpleDataSchema()));
    }

    public DataSchemaEntry<SimpleDataSchema> arrayEntryFor(Type elementType, String name, boolean mandatory){
        ArrayType entryType =  new ArrayType(elementType);
        Type elementsType = internalElementType(entryType);
        if(elementsType == TypeEnum.COMPLEXTYPE){
            return schema.computeIfAbsent(name, k -> new DataSchemaEntry<>(this, name, entryType, mandatory, new SimpleDataSchema()));
        }

        return schema.computeIfAbsent(name, k -> new DataSchemaEntry<>(this, name, entryType, mandatory, null));
    }

    public DataSchemaEntry<SimpleDataSchema> booleanArrayEntry(String name, boolean mandatory){
        return arrayEntryFor(TypeEnum.BOOLEAN, name, mandatory);
    }

    public DataSchemaEntry<SimpleDataSchema> numberArrayEntry(String name, boolean mandatory){
        return arrayEntryFor(TypeEnum.NUMBER, name, mandatory);
    }

    public DataSchemaEntry<SimpleDataSchema> stringArrayEntry(String name, boolean mandatory){
        return arrayEntryFor(TypeEnum.STRING, name, mandatory);
    }

    public DataSchemaEntry<SimpleDataSchema> objectArrayEntry(String name, boolean mandatory){
        return arrayEntryFor(TypeEnum.COMPLEXTYPE, name, mandatory);
    }



    private void addAll(SimpleDataSchema otherSchema) {
        if(otherSchema == null){
            return;
        }

        schema.putAll(otherSchema.schema);
    }


    public void unify(SimpleDataSchema otherSchema){
        otherSchema.schema.forEach((k, v) -> {
            if(this.schema.containsKey(k)){
                DataSchemaEntry<SimpleDataSchema> entry = this.schema.get(k);
                if(!entry.type.equals(v.type)){
                    throw new IllegalStateException("Types for field \"" + k + "\" are not consistent: \"" + entry.type + "\" != \"" + v.type + "\"");
                }

                if(entry.dataSchema != null){
                    entry.dataSchema.unify(v.dataSchema);
                }
            } else {
                this.schema.put(k, new DataSchemaEntry<>(this, v.name, v.type, false, v.dataSchema));
            }
        });

        this.schema.forEach((k, v) -> {
            if(!otherSchema.schema.containsKey(k)){
                this.schema.put(k, new DataSchemaEntry<>(this, v.name, v.type, false, v.dataSchema));
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleDataSchema that = (SimpleDataSchema) o;
        return Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema);
    }

    @Override
    public List<Node> applyDataToTerms(Node term, Map<Variable, Type> variableTypes) {
        List<Triplet<Node, Set<Variable>, Set<Node>>> termsToAssign = new LinkedList<>();
        termsToAssign.add(new Triplet<>(term, new HashSet<>(variableTypes.keySet()), new HashSet<>()));
        DataSchema.findAssignableFields(termsToAssign, variableTypes, this.getDataSchemaEntries(), variableNodeProvider);

        List<Node> terms = new LinkedList<>();
        for (Triplet<Node, Set<Variable>, Set<Node>> triplet : termsToAssign) {
            // Check if all variables have been assigned a field.
            if(triplet.getValue1().isEmpty()){
                terms.add(triplet.getValue0());
            }
        }

        return terms;
    }

    @Override
    public  <T extends DataSchema> DataSchemaEntry<T> getParentEntry() {
        //noinspection unchecked
        return (DataSchemaEntry<T>) parentEntry;
    }

    @Override
    public <T extends DataSchema> void setParentEntry(DataSchemaEntry<T> parentEntry) {
        //noinspection unchecked
        this.parentEntry = (DataSchemaEntry<SimpleDataSchema>) parentEntry;
    }

    @Override
    public <DS extends DataSchema, T> EvaluationResults<DS, T> evaluate(DataCollection<T> data) {
        EvaluationResults<DS, T> evaluationResults = new EvaluationResults<>();
        data.visitDataEntries((values, dataEntry) -> {
            if(!(dataEntry instanceof DataObject)){
                return;
            }

            evaluateDataObject((DataObject)dataEntry, dataEntry, data, evaluationResults);
        });

        return evaluationResults;
    }

    public <DS extends DataSchema, T> void evaluateDataObject(DataObject dao, T dataEntry, DataCollection<T> data, EvaluationResults<DS, T> evaluationResults){
        Map<Variable, Node> values = new HashMap<>();

        Map<DataSchemaEntry<SimpleDataSchema>, Set<Constraint>> constraints = new HashMap<>();
        Map<DataSchemaEntry<SimpleDataSchema>, Set<Constraint>> potentialConstraints = new HashMap<>();

        evaluateDataObject(dao, dataEntry, values, constraints, potentialConstraints, evaluationResults);

        constraints.forEach((k, v) -> {
            if(v == null || v.isEmpty()){
                return;
            }

            for (Constraint constraint : v) {
                @SuppressWarnings("unchecked")
                ConstraintResults<T> constraintResults = evaluationResults.getConstraintResults((DataSchemaEntry<DS>) k, constraint, data);
                constraint.applyData(values, dataEntry, constraintResults);
            }
        });

        potentialConstraints.forEach((k, v) -> {
            if(v == null || v.isEmpty()){
                return;
            }

            for (Constraint constraint : v) {
                @SuppressWarnings("unchecked")
                ConstraintResults<T> constraintResults = evaluationResults.getPotentialConstraintResults((DataSchemaEntry<DS>) k, constraint, data);
                constraint.applyData(values, dataEntry, constraintResults);
            }
        });
    }

    private <DS extends DataSchema, T> void evaluateDataObject(
            DataObject dao, T dataEntry,
            Map<Variable, Node> values,
            Map<DataSchemaEntry<SimpleDataSchema>,
            Set<Constraint>> constraints,
            Map<DataSchemaEntry<SimpleDataSchema>,
            Set<Constraint>> potentialConstraints,
            EvaluationResults<DS, T> evaluationResults
    ){
        for (DataSchemaEntry<SimpleDataSchema> entry : this.schema.values()) {
            DataValue<?> value = dao.getDataValue(entry.name);
            if(value == null){
                if(entry.mandatory){
                    //noinspection unchecked
                    evaluationResults.addResult((EvaluationResult<DS,T>)new MissingMandatoryValue<>(entry, dataEntry));
                }
                continue;
            }

            if(!value.getType().canAssignTo(entry.type)){
                //noinspection unchecked
                evaluationResults.addResult((EvaluationResult<DS,T>)new IncompatibleTypes<>(entry, dataEntry, value.getType()));
            }

            if(entry.type == TypeEnum.COMPLEXTYPE){
                entry.dataSchema.evaluateDataObject((DataObject) value.getValue(), dataEntry, values, constraints, potentialConstraints, evaluationResults);
//            } if(entry.type instanceof ArrayType){

            } else {
                Value<?> literal = value.getLiteralValue();
                values.put(new DataReference(entry), literal);
            }

            constraints.put(entry, entry.constraints);
            potentialConstraints.put(entry, entry.potentialConstraints);
        }
    }

    public static SimpleDataSchema deriveFromData(DataObject dao){
        SimpleDataSchema schema = new SimpleDataSchema();
        for (String fieldName : dao.getFieldNames()) {
            DataValue<?> value = dao.getDataValue(fieldName);
            schema.createEntry(fieldName, value);
        }

        return schema;
    }

    private DataSchemaEntry<SimpleDataSchema> createEntry(String fieldName, DataValue<?> value){
        if(value.getType() instanceof TypeEnum) {
            switch ((TypeEnum) value.getType()){
                case NUMBER:
                    return numberEntry(fieldName, true);
                case BOOLEAN:
                    return booleanEntry(fieldName, true);
                case STRING:
                    return stringEntry(fieldName, true);
                case COMPLEXTYPE:
                    DataObject nestedDao = (DataObject) value.getValue();
                    SimpleDataSchema nestedSchema = deriveFromData(nestedDao);

                    DataSchemaEntry<SimpleDataSchema> entry = objectEntry(fieldName, true);
                    entry.dataSchema.addAll(nestedSchema);

                    return entry;
                default :
                    throw new IllegalStateException("Unexpected value: " + value.getType());
            }
        } else if (value.getType() instanceof ArrayType){
            Type internalElementType = internalElementType((ArrayType) value.getType());
            Type elementType = ((ArrayType) value.getType()).elementType();

            if(internalElementType instanceof TypeEnum){
                switch ((TypeEnum) internalElementType){
                    case NUMBER: case BOOLEAN: case STRING:
                        return arrayEntryFor(elementType, fieldName, true);
                    case COMPLEXTYPE:
                        List<DataObject> nestedDaos = internalElements(value);
                        SimpleDataSchema nestedSchema = null;
                        for(DataObject nestedDao: nestedDaos) {
                            SimpleDataSchema schema = deriveFromData(nestedDao);
                            if(nestedSchema == null){
                                nestedSchema = schema;
                            } else {
                                nestedSchema.unify(schema);
                            }
                        }

                        DataSchemaEntry<SimpleDataSchema> entry = arrayEntryFor(elementType, fieldName, true);
                        entry.dataSchema.addAll(nestedSchema);

                        return entry;
                    default :
                        throw new IllegalStateException("Unexpected value: " + value.getType());
                }
            }
        }

        throw new IllegalStateException("Unexpected value: " + value.getType());
    }

    private static Type internalElementType(ArrayType arrayType){
        if(arrayType.elementType() instanceof ArrayType) {
            return internalElementType((ArrayType) arrayType.elementType());
        }

        return arrayType.elementType();
    }

    private static List<DataObject> internalElements(DataValue<?> value){
        List<DataObject> daos = new LinkedList<>();
        if(value.getValue() instanceof DataObject[]){
            Collections.addAll(daos, ((DataObject[]) value.getValue()));
        } else if(value.getValue() instanceof DataValue[]){
            for (DataValue<?> dataValue : ((DataValue<?>[]) value.getValue())) {
                daos.addAll(internalElements(dataValue));
            }
        }
        return daos;
    }

    @Override
    public String toString() {
        return toString("");
    }

    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        schema.forEach((k, v) -> {
            sb.append(indent);
            sb.append(v.toString(indent, "\t"));
            sb.append("\n");

            if(v.dataSchema != null){
                sb.append(v.dataSchema.toString(indent + "\t"));
            }
        });

        return sb.toString();
    }
}
