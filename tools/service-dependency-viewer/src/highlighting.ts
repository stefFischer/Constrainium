import type { Model } from './model';
export type Direction = 'upstream' | 'downstream' | 'both';
export type Depth = 'direct' | 'transitive';
export function traceFields(model: Model, active: string | null, direction: Direction, depth: Depth) {
  const fields = new Set<string>(), connections = new Set<string>(), edges = new Set<string>(), services = new Set<string>();
  if (active && model.fields.has(active)) {
    fields.add(active);
    // Both is the union of two directed walks, not an undirected walk through sibling branches.
    for (const dir of direction === 'both' ? ['upstream', 'downstream'] : [direction]) {
      const adjacency = new Map<string, typeof model.connections>();
      model.connections.forEach(c => { const key = dir === 'upstream' ? c.to : c.from; adjacency.set(key, [...(adjacency.get(key) ?? []), c]); });
      const visited = new Set([active]), queue = [active];
      for (let i = 0; i < queue.length; i++) for (const c of adjacency.get(queue[i]) ?? []) {
        const next = dir === 'upstream' ? c.from : c.to;
        connections.add(c.id); fields.add(next);
        if (depth === 'transitive' && !visited.has(next)) { visited.add(next); queue.push(next); }
      }
    }
    fields.forEach(id => edges.add(model.fields.get(id)!.edgeId));
    model.edges.forEach(e => { if (edges.has(e.id)) { services.add(e.source); services.add(e.target); } });
  }
  return { fields, connections, edges, services };
}
