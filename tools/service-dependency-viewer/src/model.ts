export type Side = 'input' | 'output' | 'schema';
export interface Field {
  id: string; entryId: string; edgeId: string; side: Side;
  path: string[]; name: string; type: string; mandatory: boolean;
  ancestors: string[]; children: string[];
}
export interface SchemaSide { side: Side; label: string; roots: string[]; missing: boolean }
export interface Edge { id: string; type: string; source: string; target: string; query?: string | null; schemas: SchemaSide[] }
export interface Connection { id: string; from: string; to: string; traceId?: string | null; flowId: string }
export interface Model {
  services: { id: string; name: string; type: string }[]; edges: Edge[];
  fields: Map<string, Field>; connections: Connection[];
}
// Tuple encoding avoids collisions from punctuation, identical paths and shared schemas.
export const fieldIdentity = (edge: string, side: Side, entries: string[]) => JSON.stringify([edge, side, entries]);
