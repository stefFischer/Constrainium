import type { Dispatch } from 'react';
import type { Action, Selection } from '../state';
export function HighlightControls({ selection, dispatch }: { selection: Selection; dispatch: Dispatch<Action> }) {
  return <div className="highlight-controls">
    <fieldset><legend>Direction</legend>{(['upstream', 'downstream', 'both'] as const).map(value => <label key={value}><input type="radio" name="direction" checked={selection.direction === value} onChange={() => dispatch({ type: 'direction', value })}/>{value === 'both' ? 'Both directions' : value[0].toUpperCase() + value.slice(1)}</label>)}</fieldset>
    <fieldset><legend>Depth</legend>{(['direct', 'transitive'] as const).map(value => <label key={value}><input type="radio" name="depth" checked={selection.depth === value} onChange={() => dispatch({ type: 'depth', value })}/>{value[0].toUpperCase() + value.slice(1)}</label>)}</fieldset>
    <button onClick={() => dispatch({ type: 'clear' })} disabled={!selection.edge}>Clear selection</button>
  </div>;
}
