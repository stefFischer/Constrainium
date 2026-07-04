package at.sfischer.traces.otel.dataextraction.sql;

public enum ParameterRole {
    SET,       // UPDATE SET col = ?
    WHERE,     // WHERE col = ?
    INSERT,    // INSERT INTO t (col) VALUES (?)
}
