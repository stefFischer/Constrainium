import { describe, expect, it } from 'vitest';
import { traceFields } from '../src/highlighting';
import type { Model, Field } from '../src/model';
const field = (id: string): Field => ({ id, entryId: id, edgeId: id, side: 'input', path: [id], name: id, type: 'string', mandatory: true, ancestors: [], children: [] });
const model: Model = {
  services: ['s', 't'].map(id => ({ id, name: id, type: 'unknown' })),
  edges: ['a', 'b', 'c', 'd', 'e', 'x'].map(id => ({ id, type: 'dependency', source: 's', target: 't', schemas: [] })),
  fields: new Map(['a', 'b', 'c', 'd', 'e', 'x'].map(id => [id, field(id)])),
  connections: [['a', 'b'], ['b', 'c'], ['c', 'd'], ['d', 'b'], ['a', 'e']].map(([from, to], i) => ({ id: String(i), flowId: String(i), from, to })),
};
describe('explicit field traversal', () => {
  it.each([
    ['upstream', 'direct', ['a', 'b', 'd']],
    ['downstream', 'direct', ['b', 'c']],
    ['both', 'direct', ['a', 'b', 'c', 'd']],
    ['upstream', 'transitive', ['a', 'b', 'c', 'd']],
    ['downstream', 'transitive', ['b', 'c', 'd']],
    ['both', 'transitive', ['a', 'b', 'c', 'd']],
  ] as const)('%s %s terminates on cycles and gives the directed result', (direction, depth, expected) => {
    const result = traceFields(model, 'b', direction, depth);
    expect([...result.fields].sort()).toEqual(expected);
    expect([...result.edges].sort()).toEqual(expected);
    expect([...result.services].sort()).toEqual(['s', 't']);
    expect(result.connections.has('4')).toBe(false); // no direction switching at ancestor a
  });
  it('does not infer sibling fields through a service or matching path', () => {
    expect([...traceFields(model, 'x', 'both', 'transitive').fields]).toEqual(['x']);
  });
  it('has no highlight without an active field', () => {
    expect(traceFields(model, null, 'both', 'direct').fields.size).toBe(0);
  });
  it('includes direct self connections once', () => {
    const m = { ...model, connections: [{ id: 'self', flowId: 'self', from: 'x', to: 'x' }] };
    expect([...traceFields(m, 'x', 'both', 'transitive').connections]).toEqual(['self']);
  });
});
