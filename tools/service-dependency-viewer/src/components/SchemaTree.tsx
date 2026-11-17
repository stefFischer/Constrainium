import { useEffect, useRef, useState } from 'react';
import type { Model, SchemaSide } from '../model';
export function SchemaTree({ schema, model, active, related, navigation, onField }: { schema: SchemaSide; model: Model; active: string | null; related: Set<string>; navigation: number; onField: (id: string) => void }) {
  const [expanded, setExpanded] = useState<Set<string>>(new Set()), [open, setOpen] = useState(true);
  const root = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const field = active ? model.fields.get(active) : undefined;
    if (!field || field.side !== schema.side) return;
    setOpen(true); setExpanded(prev => new Set([...prev, ...field.ancestors]));
  }, [active, navigation, model, schema.side]);
  useEffect(() => {
    if (!active || !open) return;
    const button = Array.from(root.current?.querySelectorAll<HTMLButtonElement>('[data-field]') ?? []).find(el => el.dataset.field === active);
    if (button) { button.scrollIntoView?.({ block: 'nearest' }); button.focus({ preventScroll: true }); }
  }, [active, navigation, expanded, open]);
  function toggle(id: string) { setExpanded(prev => { const next = new Set(prev); if (next.has(id)) next.delete(id); else next.add(id); return next; }); }
  function row(id: string): React.ReactNode {
    const f = model.fields.get(id)!, hasChildren = f.children.length > 0;
    return <li key={id}><div className={`field-row ${active === id ? 'active-field' : related.has(id) ? 'related-field' : ''}`}>
      {hasChildren ? <button className="expand" aria-label={`${expanded.has(id) ? 'Collapse' : 'Expand'} ${f.name}`} aria-expanded={expanded.has(id)} onClick={() => toggle(id)}>{expanded.has(id) ? '▾' : '▸'}</button> : <span className="leaf">·</span>}
      <button className="field" data-field={id} aria-pressed={active === id} onClick={() => onField(id)} onKeyDown={e => { if (hasChildren && (e.key === 'ArrowRight' || e.key === 'ArrowLeft')) { e.preventDefault(); setExpanded(prev => { const next = new Set(prev); if (e.key === 'ArrowRight') next.add(id); else next.delete(id); return next; }); } }}>
        <span>{f.name}<small>{f.mandatory ? ' required' : ' optional'}</small></span><code>{f.type}</code>{active === id ? <b className="field-status">● Active</b> : related.has(id) ? <b className="field-status">↔ Related</b> : null}
      </button></div>{hasChildren && expanded.has(id) && <ul>{f.children.map(row)}</ul>}</li>;
  }
  return <div className="schema-tree" ref={root}><button className="schema-heading" aria-expanded={open} onClick={() => setOpen(!open)}>{open ? '▾' : '▸'} {schema.label}<span>{schema.roots.length} root fields</span></button>
    {open && (schema.missing ? <p className="muted">No schema exported.</p> : schema.roots.length ? <ul aria-label={`${schema.label} fields`}>{schema.roots.map(row)}</ul> : <p className="muted">Schema contains no fields.</p>)}
  </div>;
}
