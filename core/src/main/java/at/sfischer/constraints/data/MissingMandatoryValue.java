package at.sfischer.constraints.data;

public class MissingMandatoryValue<T extends DataSchema, DATA> extends EvaluationResult<T, DATA> {

    public MissingMandatoryValue(DataSchemaEntry<T> schemaEntry, DATA dataEntry) {
        super(schemaEntry, dataEntry);
    }

    @Override
    public String getMessage() {
        return "Mandatory values is missing in data.";
    }
}
