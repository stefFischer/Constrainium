import { useRef, useState } from 'react';
import type { Model } from '../model';
import { downloadStyles, nodeStyleSchema, parseStyles, removeTypeStyle, setTypeStyle, shapes, styleForType, type NodeStyle, type NodeStyleConfig } from '../styles/nodeStyles';

interface Props {
  model: Model; config: NodeStyleConfig; ready: boolean; notice: string;
  onSave: (config: NodeStyleConfig) => void; onReset: () => void;
}
export function NodeStyleEditor({ model, config, ready, notice, onSave, onReset }: Props) {
  const dialog = useRef<HTMLDialogElement>(null);
  const [target, setTarget] = useState<string | null>(null), [error, setError] = useState('');
  const types = [...new Set([...model.services.map(s => s.type), ...Object.keys(config.types), ...(target === null ? [] : [target])])].sort();
  const current = target === null ? config.default : styleForType(config, target);
  async function load(file: File) {
    setError('');
    try {
      if (file.size > 1024 * 1024) throw new Error('Style file exceeds 1 MiB. Choose a smaller node styles file.');
      onSave(parseStyles(await file.text()));
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not load styles.'); }
  }
  return <>
    <button disabled={!ready} onClick={() => dialog.current?.showModal()} title={notice || 'Customize and save styles for each node type'}>Node styles{notice ? ' !' : ''}</button>
    <dialog ref={dialog} className="style-dialog" aria-labelledby="node-styles-title">
      <div className="style-dialog-heading"><div><span className="eyebrow">APPEARANCE</span><h2 id="node-styles-title">Node styles</h2></div><button onClick={() => dialog.current?.close()} aria-label="Close node styles">Close</button></div>
      <p className="muted">Saved automatically in this browser. Import or save JSON to reuse styles elsewhere.</p>
      <div className="style-file-actions">
        <label className="upload">Load styles<input aria-label="Load node styles JSON" type="file" accept=".json,application/json" onChange={e => { const file = e.target.files?.[0]; if (file) void load(file); e.target.value = ''; }}/></label>
        <button onClick={() => { try { downloadStyles(config); setError(''); } catch { setError('Styles could not be downloaded. Check browser download permissions.'); } }}>Save JSON</button>
        <button onClick={() => { onReset(); setError(''); }}>Restore startup styles</button>
      </div>
      {notice && <p className="style-notice" role="status">{notice}</p>}
      {error && <pre className="style-error" role="alert">{error}</pre>}
      <label className="style-target">Style for<select aria-label="Style for node type" value={target === null ? 'default' : JSON.stringify([target])} onChange={e => setTarget(e.target.value === 'default' ? null : JSON.parse(e.target.value)[0])}>
        <option value="default">Default style (all types)</option>
        {types.map(type => <option key={type} value={JSON.stringify([type])}>{type || '(empty type)'} · {model.services.filter(s => s.type === type).length} nodes{Object.hasOwn(config.types, type) ? ' · customized' : ''}</option>)}
      </select></label>
      <p className="muted">{target === null ? 'Used for every type without an override.' : 'Only values different from the default are stored as overrides. Other values inherit the default.'}</p>
      <StyleForm key={JSON.stringify([target, config])} style={current} onApply={style => {
        const override = Object.fromEntries(Object.entries(style).filter(([key, value]) => value !== config.default[key as keyof NodeStyle]));
        onSave(setTypeStyle(config, target, target === null ? style : override)); setError('');
      }}/>
      {target !== null && <button className="reset-type" disabled={!Object.hasOwn(config.types, target)} onClick={() => { onSave(removeTypeStyle(config, target)); setError(''); }}>Use default for this type</button>}
      <p className="muted">Style edits keep node positions and tracing intact. After increasing node sizes, use Apply again on the graph if nodes overlap.</p>
    </dialog>
  </>;
}

function StyleForm({ style, onApply }: { style: NodeStyle; onApply: (style: NodeStyle) => void }) {
  const [draft, setDraft] = useState<Record<keyof NodeStyle, string>>(() => Object.fromEntries(Object.entries(style).map(([key, value]) => [key, String(value)])) as Record<keyof NodeStyle, string>);
  const [error, setError] = useState('');
  const set = (key: keyof NodeStyle, value: string) => setDraft(s => ({ ...s, [key]: value }));
  return <form onSubmit={e => {
    e.preventDefault();
    const result = nodeStyleSchema.safeParse({ ...draft, width: Number(draft.width), height: Number(draft.height), fontSize: Number(draft.fontSize), borderWidth: Number(draft.borderWidth) });
    if (!result.success) { setError(result.error.issues.map(i => `${i.path.join('.')}: ${i.message}`).join('\n')); return; }
    onApply(result.data); setError('');
  }}>
    <div className="style-form-grid">
      <label>Shape<select aria-label="Shape" value={draft.shape} onChange={e => set('shape', e.target.value)}>{shapes.map(shape => <option key={shape} value={shape}>{shape}</option>)}</select></label>
      {([['fill', 'Fill color'], ['border', 'Border color'], ['text', 'Text color']] as const).map(([key, label]) => <label key={key}>{label}<div className="style-color-input"><input type="color" aria-label={`Pick ${label.toLowerCase()}`} value={/^#[0-9a-fA-F]{6}$/.test(draft[key]) ? draft[key] : style[key]} onChange={e => set(key, e.target.value)}/><input aria-label={label} type="text" required pattern="#[0-9a-fA-F]{6}" value={draft[key]} onChange={e => set(key, e.target.value)}/></div></label>)}
      {([['width', 'Width', 60, 400], ['height', 'Height', 30, 240], ['fontSize', 'Font size', 10, 32], ['borderWidth', 'Border width', 0, 8]] as const).map(([key, label, min, max]) => <label key={key}>{label}<input type="number" min={min} max={max} step={key === 'borderWidth' ? 0.5 : 1} required value={draft[key]} onChange={e => set(key, e.target.value)}/></label>)}
    </div>
    {error && <pre className="style-error" role="alert">{error}</pre>}
    <button className="apply-style" type="submit">Apply style</button>
  </form>;
}
