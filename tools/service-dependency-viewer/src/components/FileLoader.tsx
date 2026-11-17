import { useRef, useState } from 'react';
import { parseExport } from '../data/adapter';
import type { Model } from '../model';
export function FileLoader({ onLoad }: { onLoad: (model: Model, name: string) => void }) {
  const [error, setError] = useState(''), [busy, setBusy] = useState(false), sequence = useRef(0);
  async function load(read: () => Promise<string>, name: string) {
    const request = ++sequence.current; setBusy(true); setError('');
    try { const model = parseExport(await read()); if (request === sequence.current) onLoad(model, name); }
    catch (e) { if (request === sequence.current) setError(e instanceof Error ? e.message : 'Unable to read this file.'); }
    finally { if (request === sequence.current) setBusy(false); }
  }
  return <div className="loader">
    <div className="load-actions"><button disabled={busy} onClick={() => load(async () => { const r = await fetch(`${import.meta.env.BASE_URL}sample.json`); if (!r.ok) throw new Error('Sample unavailable. Load public/sample.json using the file picker.'); return r.text(); }, 'sample.json')}>Load sample</button>
    <label className="upload">Open JSON<input type="file" accept=".json,application/json" disabled={busy} aria-label="Open GraphJsonExporter JSON file" onChange={e => { const file = e.target.files?.[0]; if (file) { if (file.size > 25 * 1024 * 1024) setError('File exceeds 25 MiB. Export a smaller graph.'); else void load(() => file.text(), file.name); } e.target.value = ''; }}/></label></div>
    <span className="privacy">{busy ? 'Reading graph…' : 'Local processing · No uploads'}</span>
    {error && <div className="validation" role="alert"><strong>Could not load graph</strong><pre>{error}</pre><span>Your current graph is preserved.</span></div>}
  </div>;
}
