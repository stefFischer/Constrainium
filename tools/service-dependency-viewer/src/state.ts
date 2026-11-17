import type { Model } from './model';
import type { Direction, Depth } from './highlighting';
export interface Selection { edge: string | null; field: string | null; direction: Direction; depth: Depth; navigation: number }
export const initialSelection: Selection = { edge: null, field: null, direction: 'both', depth: 'transitive', navigation: 0 };
export type Action = { type: 'edge'; id: string } | { type: 'field'; id: string; model: Model } | { type: 'clear' } | { type: 'reset' } | { type: 'direction'; value: Direction } | { type: 'depth'; value: Depth };
export function selectionReducer(s: Selection, a: Action): Selection {
  switch (a.type) {
    case 'edge': return { ...s, edge: a.id, field: null };
    case 'field': { const f = a.model.fields.get(a.id); return f ? { ...s, edge: f.edgeId, field: f.id, navigation: s.navigation + 1 } : s; }
    case 'direction': return { ...s, direction: a.value };
    case 'depth': return { ...s, depth: a.value };
    case 'clear': return { ...s, edge: null, field: null };
    case 'reset': return initialSelection;
  }
}
