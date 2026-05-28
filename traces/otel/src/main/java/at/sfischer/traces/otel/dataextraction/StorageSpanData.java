package at.sfischer.traces.otel.dataextraction;

import at.sfischer.constraints.data.DataObject;

import java.util.Set;

public class StorageSpanData extends SpanData {

    private final StorageOperation operation;

    private final Set<String> affectedData;

    private final Set<String> selectors;

    public StorageSpanData(DataObject inputData, DataObject outputData, StorageOperation operation, Set<String> affectedData, Set<String> selectors) {
        super(inputData, outputData);
        this.operation = operation;
        this.affectedData = affectedData;
        this.selectors = selectors;
    }

    public StorageOperation getOperation() {
        return operation;
    }

    public Set<String> getAffectedData() {
        return affectedData;
    }

    public Set<String> getSelectors() {
        return selectors;
    }
}
