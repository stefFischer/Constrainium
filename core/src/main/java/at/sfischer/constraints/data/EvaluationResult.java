package at.sfischer.constraints.data;

import java.util.Objects;

public abstract class EvaluationResult<SCHEMA extends DataSchema, DATA> {

    private final DataSchemaEntry<SCHEMA> schemaEntry;

    private final DATA dataEntry;

    public EvaluationResult(DataSchemaEntry<SCHEMA> schemaEntry, DATA dataEntry) {
        this.schemaEntry = schemaEntry;
        this.dataEntry = dataEntry;
    }

    public DataSchemaEntry<SCHEMA> getSchemaEntry() {
        return schemaEntry;
    }

    public DATA getDataEntry() {
        return dataEntry;
    }

    public abstract String getMessage();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EvaluationResult<?, ?> that = (EvaluationResult<?, ?>) o;
        return Objects.equals(schemaEntry, that.schemaEntry) && Objects.equals(dataEntry, that.dataEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaEntry, dataEntry);
    }

    @Override
    public String toString() {
        return this.schemaEntry.getQualifiedName() + ": " + getMessage();
    }
}
