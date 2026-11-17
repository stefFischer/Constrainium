import type { Model } from '../model';
import type { Selection } from '../state';
import { SchemaTree } from './SchemaTree';
export function EdgeDetails({ model, selection, related, onEdge, onField }: { model: Model; selection: Selection; related: Set<string>; onEdge: (id: string) => void; onField: (id: string) => void }) {
  const edge = model.edges.find(e => e.id === selection.edge);
  const name = (id: string) => model.services.find(s => s.id === id)?.name || id;
  return <section className="details" aria-label="Selected edge details"><div className="panel-heading"><div><span className="eyebrow">02 / INSPECT</span><h2>Edge details</h2></div>{edge && <span className="badge">Selected</span>}</div>
    <div className="detail-body"><label className="edge-picker">Select an edge<select aria-label="Select an edge" value={selection.edge ?? ''} onChange={e => onEdge(e.target.value)}><option value="" disabled>Choose a graph edge</option>{model.edges.map(e => <option key={e.id} value={e.id}>{e.id} · {name(e.source)} → {name(e.target)} · {e.type}</option>)}</select></label>
    {edge ? <><div className="edge-meta"><code>{edge.id}</code><span className="badge">{edge.type}</span><p>{name(edge.source)} <span aria-label="to">→</span> {name(edge.target)}</p><small>{edge.source} → {edge.target}</small></div>
      {edge.query != null && <div className="query"><span className="eyebrow">QUERY</span><pre>{edge.query || '(Empty query)'}</pre></div>}
      <p className="muted">Select a field to trace its explicit data connections.</p>
      {edge.schemas.map(s => <SchemaTree key={`${edge.id}:${s.side}`} schema={s} model={model} active={selection.field} related={related} navigation={selection.navigation} onField={onField}/>)}</> : <div className="empty-details"><span>↗</span><h3>Follow a dependency</h3><p>Select an edge on the graph or from the list above to inspect its schemas.</p></div>}
    </div></section>;
}
