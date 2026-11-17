import { documentSchema, type ExportDocument, type ExportType } from './exportFormat';
import { fieldIdentity, type Model, type Field, type Side, type SchemaSide } from '../model';

export class ImportError extends Error {}
function fail(message: string): never { throw new ImportError(message); }
function index<T extends { id: string }>(rows: T[], table: string) {
  const map = new Map<string, T>();
  rows.forEach(row => { if (map.has(row.id)) fail(`${table}: duplicate ID "${row.id}". Export unique records.`); map.set(row.id, row); });
  return map;
}
const typeLabel = (t: ExportType): string => t.kind === 'ARRAY' ? `${typeLabel(t.elementType)}[]` : t.kind.toLowerCase();
export function adapt(d: ExportDocument): Model {
  const nodes = index(d.nodes, 'nodes'), edges = index(d.edges, 'edges'), schemas = index(d.schemas, 'schemas'), entries = index(d.schemaEntries, 'schemaEntries'), flows = index(d.dataFlows, 'dataFlows');
  index(d.objects, 'objects');
  function check(map: Map<string, unknown>, id: string | null | undefined, at: string) {
    if (id != null && !map.has(id)) fail(`${at}: unknown reference "${id}". Re-export the complete graph.`);
  }
  d.entryNodes.forEach(id => check(nodes, id, 'entryNodes'));
  d.nodes.forEach(n => {
    for (const [dir, ids] of [['from', n.outgoing], ['to', n.incoming]] as const) ids.forEach(id => {
      check(edges, id, `node ${n.id}`);
      if (edges.get(id)![dir] !== n.id) fail(`node ${n.id}: ${id} has inconsistent adjacency.`);
    });
  });
  d.schemas.forEach(s => {
    check(entries, s.parentEntry, `schema ${s.id}.parentEntry`);
    if (s.kind === 'simple') s.entries.forEach(id => check(entries, id, `schema ${s.id}.entries`));
    else { check(schemas, s.input, `schema ${s.id}.input`); check(schemas, s.output, `schema ${s.id}.output`); }
  });
  d.schemaEntries.forEach(e => { check(schemas, e.parentSchema, `field ${e.id}.parentSchema`); check(schemas, e.nestedSchema, `field ${e.id}.nestedSchema`); });
  const fields = new Map<string, Field>();
  const normalizedEdges = d.edges.map(e => {
    check(nodes, e.from, `edge ${e.id}.from`); check(nodes, e.to, `edge ${e.id}.to`); check(schemas, e.schema, `edge ${e.id}.schema`);
    e.dataFlows?.forEach(id => check(flows, id, `edge ${e.id}.dataFlows`));
    const root = e.schema ? schemas.get(e.schema) : undefined;
    if (root && ((e.kind === 'asynchronous-call' && root.kind !== 'simple') || ((e.kind === 'synchronous-call' || e.kind === 'database-query') && root.kind !== 'input-output'))) fail(`edge ${e.id}: schema kind does not match ${e.kind}.`);
    function walk(schemaId: string | null, side: Side, path: string[] = [], chain: string[] = [], ancestors: string[] = [], visited = new Set<string>()): string[] {
      if (!schemaId) return [];
      if (visited.has(schemaId) || visited.size > 100) fail(`edge ${e.id}: cyclic or excessively deep schema containment at ${schemaId}. Parent back-references are supported; recursive field trees are not.`);
      const seen = new Set(visited).add(schemaId), s = schemas.get(schemaId)!;
      if (s.kind !== 'simple') fail(`edge ${e.id}: nested input-output schemas cannot be assigned a unique schema side.`);
      return s.entries.map(entryId => {
        const entry = entries.get(entryId)!;
        const ids = [...chain, entryId], id = fieldIdentity(e.id, side, ids);
        if (fields.has(id)) fail(`schema ${schemaId}: repeated entry ${entryId}.`);
        const f: Field = { id, entryId, edgeId: e.id, side, path: [...path, entry.name], name: entry.name, type: typeLabel(entry.type), mandatory: entry.mandatory, ancestors, children: [] };
        fields.set(id, f);
        f.children = walk(entry.nestedSchema, side, [...path, entry.name + '[]'.repeat(arrayDepth(entry.type))], ids, [...ancestors, id], seen);
        return id;
      });
    }
    const sides: SchemaSide[] = root?.kind === 'input-output'
      ? [{ side: 'input', label: `Input · ${root.inputPrefix}`, roots: walk(root.input, 'input'), missing: !root.input }, { side: 'output', label: `Output · ${root.outputPrefix}`, roots: walk(root.output, 'output'), missing: !root.output }]
      : (e.kind === 'synchronous-call' || e.kind === 'database-query')
        ? [{ side: 'input', label: 'Input', roots: [], missing: true }, { side: 'output', label: 'Output', roots: [], missing: true }]
        : [{ side: 'schema', label: 'Schema', roots: walk(e.schema ?? null, 'schema'), missing: !root }];
    return { id: e.id, type: e.kind, source: e.from, target: e.to, query: e.query, schemas: sides };
  });
  const occurrences = new Map<string, string[]>();
  fields.forEach(f => { const key = JSON.stringify([f.edgeId, f.entryId]); occurrences.set(key, [...(occurrences.get(key) ?? []), f.id]); });
  const connections = d.dataFlows.flatMap(flow => {
    check(edges, flow.from, `flow ${flow.id}.from`); check(edges, flow.to, `flow ${flow.id}.to`);
    function endpoint(edge: string, entry: string) {
      const matches = occurrences.get(JSON.stringify([edge, entry])) ?? [];
      if (matches.length !== 1) fail(`flow ${flow.id}: field ${entry} has ${matches.length} occurrences on edge ${edge}. Each endpoint must identify exactly one schema side and path; use distinct schema instances and re-export.`);
      return matches[0];
    }
    return flow.mappings.map((m, i) => ({ id: JSON.stringify([flow.id, i]), flowId: flow.id, traceId: flow.traceId, from: endpoint(flow.from, m.from), to: endpoint(flow.to, m.to) }));
  });
  return { services: d.nodes.map(n => ({ id: n.id, name: n.name })), edges: normalizedEdges, fields, connections };
}
function arrayDepth(type: ExportType): number { return type.kind === 'ARRAY' ? 1 + arrayDepth(type.elementType) : 0; }
export function parseExport(text: string): Model {
  let value: unknown;
  try { value = JSON.parse(text); } catch { fail('Invalid JSON. Choose an unmodified UTF-8 GraphJsonExporter export.'); }
  const result = documentSchema.safeParse(value);
  if (!result.success) fail(result.error.issues.slice(0, 12).map(i => `${i.path.join('.') || 'document'}: ${i.message}`).join('\n'));
  return adapt(result.data);
}
