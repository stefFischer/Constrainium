package at.sfischer.traces.otel.dataextraction.sql;

public record ParameterBinding(
        int index,
        String column,
        ParameterRole role,
        Object value
) {}

