package at.sfischer.constraints.data;

import at.sfischer.constraints.model.Type;

import java.util.Objects;

public class IncompatibleTypes<T extends DataSchema, DATA> extends EvaluationResult<T, DATA> {

    private final Type wrongEntryType;

    public IncompatibleTypes(DataSchemaEntry<T> schemaEntry, DATA dataEntry, Type wrongEntryType) {
        super(schemaEntry, dataEntry);
        this.wrongEntryType = wrongEntryType;
    }

    public Type getWrongEntryType() {
        return wrongEntryType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        IncompatibleTypes<?, ?> that = (IncompatibleTypes<?, ?>) o;
        return Objects.equals(wrongEntryType, that.wrongEntryType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), wrongEntryType);
    }

    @Override
    public String getMessage() {
        return "Data has an incompatible type: " + wrongEntryType + " cannot be assigned to: " + this.getSchemaEntry().type;
    }
}
