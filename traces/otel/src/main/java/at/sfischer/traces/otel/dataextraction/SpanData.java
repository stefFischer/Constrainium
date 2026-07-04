package at.sfischer.traces.otel.dataextraction;

import at.sfischer.constraints.data.DataObject;

public class SpanData {

    private final DataObject inputData;

    private final DataObject outputData;

    public SpanData(DataObject inputData, DataObject outputData) {
        this.inputData = inputData;
        this.outputData = outputData;
    }

    public DataObject getInputData() {
        return inputData;
    }

    public DataObject getOutputData() {
        return outputData;
    }
}
