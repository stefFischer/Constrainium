package at.sfischer.constraints.model;

import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.model.validation.ValidationContext;

import java.util.Map;
import java.util.Objects;

public class DataReference extends Variable {

    private final DataSchemaEntry<?> dataSchemaEntry;

    public DataReference(DataSchemaEntry<?> dataSchemaEntry) {
        super(dataSchemaEntry.getQualifiedName());
        this.dataSchemaEntry = dataSchemaEntry;
    }

    public DataSchemaEntry<?> getDataSchemaEntry() {
        return dataSchemaEntry;
    }

    @Override
    public void validate(ValidationContext context) {
        if(this.dataSchemaEntry == null)
            context.error(this,"Reference to data schema entry cannot be null.");
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        Node value = values.get(this);
        if(value != null){
            return value;
        }

        return new DataReference(dataSchemaEntry);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DataReference that = (DataReference) o;
        return Objects.equals(dataSchemaEntry, that.dataSchemaEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataSchemaEntry);
    }

    @Override
    public String toString() {
        return "DataReference{" + dataSchemaEntry.getQualifiedName() + '}';
    }
}
