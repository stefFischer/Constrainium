import { useEffect, useRef, useState } from 'react';
import type { Core } from 'cytoscape';
import type { Model } from '../model';
import type { Selection } from '../state';
import type { traceFields } from '../highlighting';
import { createGraph, updateGraph } from '../graph/cytoscape';
import { defaultLayout, layouts, runLayout, type LayoutId } from '../graph/layouts';
export function GraphCanvas({ model, selection, highlight, onSelect }: { model: Model; selection: Selection; highlight: ReturnType<typeof traceFields>; onSelect: (id: string) => void }) {
  const container = useRef<HTMLDivElement>(null), instance = useRef<Core | null>(null), callback = useRef(onSelect);
  const [layoutId, setLayoutId] = useState<LayoutId>(defaultLayout), [revision, setRevision] = useState(0);
  const currentLayout = layouts.find(l => l.id === layoutId)!;
  callback.current = onSelect;
  useEffect(() => {
    const graph = createGraph(container.current!, model, id => callback.current(id)); instance.current = graph.cy;
    return () => { instance.current = null; graph.destroy(); };
  }, [model]);
  useEffect(() => {
    const layout = instance.current ? runLayout(instance.current, layoutId) : undefined;
    return () => { layout?.stop(); };
  }, [model, layoutId, revision]);
  useEffect(() => { if (instance.current) updateGraph(instance.current, selection, highlight); }, [model, selection, highlight]);
  function zoom(factor: number) { const cy = instance.current; if (cy) cy.zoom({ level: cy.zoom() * factor, renderedPosition: { x: cy.width() / 2, y: cy.height() / 2 } }); }
  return <section className="graph-panel" aria-label="Service dependency graph">
    <div className="panel-heading">
      <div><span className="eyebrow">01 / TOPOLOGY</span><h2>Service dependencies</h2></div>
      <div className="graph-tools">
        <div className="layout-controls">
          <label>Layout<select aria-label="Graph layout" aria-describedby="layout-description" title={currentLayout.description} value={layoutId} onChange={e => setLayoutId(e.target.value as LayoutId)}>{layouts.map(l => <option key={l.id} value={l.id}>{l.label}</option>)}</select></label>
          <button disabled={!model.services.length} onClick={() => setRevision(r => r + 1)} title="Rearrange all services using the selected layout and fit the graph">Apply again</button>
          <span id="layout-description" className="visually-hidden" role="status">{currentLayout.description}</span>
        </div>
        <div className="viewport-controls"><button onClick={() => zoom(1.25)} aria-label="Zoom in">+</button><button onClick={() => zoom(.8)} aria-label="Zoom out">−</button><button onClick={() => instance.current?.fit(undefined, 55)}>Fit view</button></div>
      </div>
    </div>
    <div className="graph" ref={container} role="img" aria-label={`${model.services.length} services and ${model.edges.length} directed edges. Use the edge selector in the details panel for keyboard access.`}/>
    {!model.services.length && <div className="empty-graph">Load an export to explore its dependencies.</div>}
    <div className="graph-legend"><span>━ Selected edge</span><span>┄ Related edge</span><span>Drag to pan · Scroll to zoom</span></div>
  </section>;
}
