import { useEffect, useState } from 'react';
import { builtInStyles, parseStyles, serializeStyles, styleStorageKey, validateStyles, type NodeStyleConfig } from './nodeStyles';

export function useNodeStyles() {
  const [config, setConfig] = useState(builtInStyles), [startup, setStartup] = useState(builtInStyles);
  const [ready, setReady] = useState(false), [notice, setNotice] = useState('');
  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      let initial = builtInStyles;
      const messages: string[] = [];
      try {
        const response = await fetch(`${import.meta.env.BASE_URL}node-styles.json`, { signal: controller.signal, cache: 'no-store' });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        initial = parseStyles(await response.text());
      } catch (e) {
        if (controller.signal.aborted) return;
        messages.push(`Startup styles could not be loaded; using the built-in style. ${e instanceof Error ? e.message : ''}`);
      }
      const startupConfig = initial;
      try { const saved = localStorage.getItem(styleStorageKey); if (saved) initial = parseStyles(saved); }
      catch { messages.push('Saved browser styles could not be read. Startup styles are being used.'); }
      if (!controller.signal.aborted) { setStartup(startupConfig); setConfig(initial); setNotice(messages.join('\n')); setReady(true); }
    }
    void load();
    return () => controller.abort();
  }, []);

  function save(value: NodeStyleConfig) {
    const valid = validateStyles(value); setConfig(valid);
    try { localStorage.setItem(styleStorageKey, serializeStyles(valid)); setNotice(''); }
    catch { setNotice('Styles applied for this session, but browser storage is unavailable. Save JSON to keep a copy.'); }
  }
  function reset() {
    setConfig(startup);
    try { localStorage.removeItem(styleStorageKey); setNotice(''); }
    catch { setNotice('Startup styles restored for this session, but the saved browser styles could not be removed.'); }
  }
  return { config, ready, notice, save, reset };
}
