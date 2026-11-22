package at.sfischer.constraints.data.model;

/** An observed pairing of collection rows on two edges within the same trace. */
public record CallRelation(String traceId, CallEdge source, int sourceRow, CallEdge target, int targetRow) {}
