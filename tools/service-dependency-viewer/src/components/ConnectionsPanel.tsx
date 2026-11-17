import type { Model } from '../model';
import type { traceFields } from '../highlighting';
export function ConnectionsPanel({ model, active, highlight, onField }: { model: Model; active: string | null; highlight: ReturnType<typeof traceFields>; onField: (id: string) => void }) {
  const rows = model.connections.filter(c => active ? highlight.connections.has(c.id) : true);
  const ref = (id: string) => { const f = model.fields.get(id)!; return <button className={`field-ref ${id === active ? 'selected-ref' : ''}`} onClick={() => onField(id)}><code>{f.edgeId}</code><span>{f.side} / {f.path.map(p => JSON.stringify(p)).join(' → ')}</span>{id === active && <b>Active</b>}</button>; };
  return <section className="connections" aria-label="Field connections"><div className="panel-heading"><div><span className="eyebrow">03 / TRACE</span><h2>Field connections <span className="count">{rows.length}</span></h2></div><span className="muted">{active ? 'Connections in the selected traversal' : 'All explicit connections'} · Select a reference to reveal its field</span></div>
    <div className="connection-scroll">{rows.length ? <table><thead><tr><th>Source edge.field</th><th aria-label="direction">→</th><th>Target edge.field</th><th>Provenance</th></tr></thead><tbody>{rows.map(c => <tr key={c.id}><td>{ref(c.from)}</td><td aria-label="flows to">→</td><td>{ref(c.to)}</td><td><code>{c.flowId}</code><small>{c.traceId ?? 'No trace ID'}</small></td></tr>)}</tbody></table> : <p className="empty-connections">{active ? 'No explicit field connections in this direction.' : 'No field connections exported. Load a graph or select a field to begin.'}</p>}</div>
  </section>;
}
