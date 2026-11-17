import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useReducer } from 'react';
import sample from '../public/sample.json';
import { parseExport } from '../src/data/adapter';
import { initialSelection, selectionReducer } from '../src/state';
import { traceFields } from '../src/highlighting';
import { EdgeDetails } from '../src/components/EdgeDetails';
import { ConnectionsPanel } from '../src/components/ConnectionsPanel';
import { HighlightControls } from '../src/components/HighlightControls';
const model = parseExport(JSON.stringify(sample));
afterEach(cleanup);
function Harness() {
  const [selection, dispatch] = useReducer(selectionReducer, initialSelection);
  const highlight = traceFields(model, selection.field, selection.direction, selection.depth);
  const onField = (id: string) => dispatch({ type: 'field', model, id });
  return <><HighlightControls selection={selection} dispatch={dispatch}/><EdgeDetails model={model} selection={selection} related={highlight.fields} onEdge={id => dispatch({ type: 'edge', id })} onField={onField}/><ConnectionsPanel model={model} active={selection.field} highlight={highlight} onField={onField}/></>;
}
describe('linked field navigation', () => {
  it('selects its containing edge, reveals nested ancestors and focuses the exact schema side', async () => {
    const user = userEvent.setup(); render(<Harness/>);
    const target = [...model.fields.values()].find(f => f.path.join('.') === 'rows[].id')!;
    const row = model.connections.find(c => c.from === target.id)!;
    const buttons = within(screen.getByRole('region', { name: 'Field connections' })).getAllByRole('button');
    const button = buttons.find(b => b.textContent?.includes(target.edgeId) && b.textContent?.includes('rows[]'))!;
    await user.click(button);
    expect((screen.getByLabelText('Select an edge') as HTMLSelectElement).value).toBe(target.edgeId);
    const active = screen.getByRole('button', { pressed: true });
    expect(active.dataset.field).toBe(target.id);
    expect(document.activeElement).toBe(active);
    expect(screen.getByRole('button', { name: 'Collapse rows' }).getAttribute('aria-expanded')).toBe('true');
    expect(screen.getByRole('button', { name: /Output · output/ }).getAttribute('aria-expanded')).toBe('true');
    expect(screen.getByRole('region', { name: 'Field connections' }).textContent).toContain(row.flowId);
    await user.click(screen.getByRole('button', { name: 'Clear selection' }));
    expect(screen.queryByRole('button', { pressed: true })).toBeNull();
    expect(screen.getByText('Follow a dependency')).toBeTruthy();
  });
  it('keeps identical input/output field names separate and reopens a closed side', async () => {
    const user = userEvent.setup(); render(<Harness/>);
    const field = [...model.fields.values()].find(f => f.side === 'output' && f.name === 'id' && f.path.length === 1)!;
    await user.selectOptions(screen.getByLabelText('Select an edge'), field.edgeId);
    const side = screen.getByRole('button', { name: /Output · response/ });
    await user.click(side); expect(side.getAttribute('aria-expanded')).toBe('false');
    const connections = screen.getByRole('region', { name: 'Field connections' });
    const link = within(connections).getAllByRole('button').find(b => b.textContent?.includes(field.edgeId) && b.textContent?.includes('output / "id"'))!;
    await user.click(link);
    expect(side.getAttribute('aria-expanded')).toBe('true');
    expect(screen.getByRole('button', { pressed: true }).dataset.field).toBe(field.id);
  });
  it('resets all selection and mode state on a valid replacement', () => {
    const s = selectionReducer({ edge: 'e', field: 'f', direction: 'upstream', depth: 'transitive', navigation: 9 }, { type: 'reset' });
    expect(s).toEqual(initialSelection);
  });
});
