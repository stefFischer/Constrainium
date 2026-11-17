import { describe, expect, it } from 'vitest';
import sample from '../public/sample.json';
import { parseExport } from '../src/data/adapter';
import { fieldIdentity } from '../src/model';

const load = (value: unknown = sample) => parseExport(JSON.stringify(value));
describe('export validation and adapter', () => {
  it('loads a real exporter snapshot with parallel edges, loops, disconnected services and missing schemas', () => {
    const model = load();
    expect(model.services).toHaveLength(6);
    expect(new Set(model.edges.map(e => e.type)).size).toBe(4);
    expect(model.edges.filter(e => e.source === 'gateway' && e.target === 'orders')).toHaveLength(2);
    expect(model.edges.some(e => e.source === e.target)).toBe(true);
    expect(model.edges.some(e => e.schemas.some(s => s.missing))).toBe(true);
    expect(model.connections).toHaveLength(5);
    const sku = [...model.fields.values()].find(f => f.name === 'sku' && f.ancestors.length)!;
    expect(sku.path).toEqual(['products[]', 'sku']);
    expect(sku.side).toBe('input');
    expect([...model.fields.values()].find(f => f.name === 'matrix')?.type).toBe('integer[][]');
  });
  it.each([
    ['version', (d: typeof sample) => { d.version = 2; }, /version/],
    ['dangling endpoint', (d: typeof sample) => { d.edges[0].from = 'missing'; }, /unknown reference|inconsistent adjacency/],
    ['duplicate ID', (d: typeof sample) => { d.nodes.push(d.nodes[0]); }, /duplicate ID/],
    ['missing field table', (d: typeof sample) => { Reflect.deleteProperty(d, 'schemaEntries'); }, /schemaEntries/],
    ['unknown mapping', (d: typeof sample) => { d.dataFlows[0].mappings[0].from = 'absent'; }, /0 occurrences/],
    ['bad type', (d: typeof sample) => { d.schemaEntries[0].type.kind = 'UNKNOWN'; }, /schemaEntries/],
    ['schema cycle', (d: typeof sample) => { const s = d.schemas.find(s => s.kind === 'simple' && s.entries?.length)!; const f = d.schemaEntries.find(f => f.id === s.entries![0])!; f.nestedSchema = s.id; }, /cyclic/],
  ])('rejects %s with an actionable error', (_label, mutate, message) => {
    const copy = structuredClone(sample); mutate(copy); expect(() => load(copy)).toThrow(message);
  });
  it('rejects invalid JSON and unsupported format', () => {
    expect(() => parseExport('{')).toThrow('Invalid JSON');
    expect(() => load({})).toThrow('format');
  });
  it('supports an empty graph', () => {
    expect(load({ format: sample.format, version: 1, entryNodes: [], nodes: [], edges: [], schemas: [], schemaEntries: [], dataFlows: [], objects: [] }).fields.size).toBe(0);
  });
  it('never collides on punctuation, side, edge or entry-chain boundaries', () => {
    const ids = [fieldIdentity('a', 'input', ['b.c']), fieldIdentity('a', 'input', ['b', 'c']), fieldIdentity('a', 'output', ['b.c']), fieldIdentity('other', 'input', ['b.c'])];
    expect(new Set(ids).size).toBe(4);
  });
  it('uses exact IDs even when qualified names are equal or misleading', () => {
    const copy = structuredClone(sample); copy.schemaEntries.forEach(f => f.qualifiedName = 'same.path');
    const m = load(copy);
    expect(m.connections).toHaveLength(5);
    expect(new Set([...m.fields.keys()]).size).toBe(m.fields.size);
  });
  it('rejects ambiguous shared input/output entries rather than choosing a side', () => {
    const copy = structuredClone(sample);
    const model = load();
    const mapping = model.connections.find(c => model.fields.get(c.from)?.side === 'input')!;
    const flow = copy.dataFlows.find(f => f.id === mapping.flowId)!;
    const edge = copy.edges.find(e => e.id === flow.from)!;
    const schema = copy.schemas.find(s => s.id === edge.schema)!;
    schema.output = schema.input;
    copy.dataFlows = [flow];
    copy.edges.forEach(e => { if (e.dataFlows) e.dataFlows = [flow.id]; });
    expect(() => load(copy)).toThrow(/2 occurrences/);
  });
  it('uses flow direction independently of owning edges', () => {
    const copy = structuredClone(sample);
    copy.edges.forEach(e => { if (e.dataFlows) e.dataFlows = copy.dataFlows.map(f => f.id); });
    expect(load(copy).connections).toEqual(load().connections);
  });
});
