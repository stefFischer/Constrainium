package at.sfischer.constraints.data;

import at.sfischer.constraints.model.*;
import org.javatuples.Triplet;

import java.util.*;

public class SimpleDataSchema extends DataSchema {

    private final Map<String, DataSchemaEntry<SimpleDataSchema>> schema;

    @Override
    protected Collection<DataSchemaEntry<?>> getDataSchemaEntries() {
        return new HashSet<>(schema.values());
    }

    public SimpleDataSchema() {
        this.schema = new HashMap<>();
    }

    public DataSchemaEntry<SimpleDataSchema> booleanEntry(String name, boolean mandatory){
        return schema.computeIfAbsent(name, k -> new DataSchemaEntry<>(name, TypeEnum.BOOLEAN, mandatory, null));
    }

    public DataSchemaEntry<SimpleDataSchema> numberEntry(String name, boolean mandatory){
        return schema.computeIfAbsent(name, k -> new DataSchemaEntry<>(name, TypeEnum.NUMBER, mandatory, null));
    }

    public DataSchemaEntry<SimpleDataSchema> stringEntry(String name, boolean mandatory){
        return schema.computeIfAbsent(name, k -> new DataSchemaEntry<>(name, TypeEnum.STRING, mandatory, null));
    }

    public DataSchemaEntry<SimpleDataSchema> objectEntry(String name, boolean mandatory){
        return schema.computeIfAbsent(name, k -> new DataSchemaEntry<>(name, TypeEnum.COMPLEXTYPE, mandatory, new SimpleDataSchema()));
    }

    public DataSchemaEntry<SimpleDataSchema> arrayEntryFor(Type elementType, String name, boolean mandatory){
        ArrayType entryType =  new ArrayType(elementType);
        Type elementsType = internalElementType(entryType);
        if(elementsType == TypeEnum.COMPLEXTYPE){
            return schema.computeIfAbsent(name, k -> new DataSchemaEntry<>(name, entryType, mandatory, new SimpleDataSchema()));
        }

        return schema.computeIfAbsent(name, k -> new DataSchemaEntry<>(name, entryType, mandatory, null));
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
                this.schema.put(k, new DataSchemaEntry<>(v.name, v.type, false, v.dataSchema));
            }
        });

        this.schema.forEach((k, v) -> {
            if(!otherSchema.schema.containsKey(k)){
                this.schema.put(k, new DataSchemaEntry<>(v.name, v.type, false, v.dataSchema));
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
            sb.append(v.toString());
            sb.append("\n");

            if(v.dataSchema != null){
                sb.append(v.dataSchema.toString(indent + "\t"));
            }
        });

        return sb.toString();
    }
}
