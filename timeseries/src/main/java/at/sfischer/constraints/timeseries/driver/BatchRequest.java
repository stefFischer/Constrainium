package at.sfischer.constraints.timeseries.driver;

import at.sfischer.constraints.data.DataObject;

import java.util.List;

public record BatchRequest(
        DataObject input,
        List<DataObject> originalInputs
) {}
