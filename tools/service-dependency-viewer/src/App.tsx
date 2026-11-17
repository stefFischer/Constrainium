import { useMemo, useReducer, useState } from 'react';
import type { Model } from './model';
import { initialSelection, selectionReducer } from './state';
import { traceFields } from './highlighting';
import { FileLoader } from './components/FileLoader';
import { HighlightControls } from './components/HighlightControls';
import { GraphCanvas } from './components/GraphCanvas';
import { EdgeDetails } from './components/EdgeDetails';
import { ConnectionsPanel } from './components/ConnectionsPanel';
import { ResizableLayout } from './components/ResizableLayout';
const empty: Model = { services: [], edges: [], fields: new Map(), connections: [] };
export default function App() {
  const [model, setModel] = useState<Model>(empty), [filename, setFilename] = useState('No export loaded');
  const [selection, dispatch] = useReducer(selectionReducer, initialSelection);
  const highlight = useMemo(() => traceFields(model, selection.field, selection.direction, selection.depth), [model, selection.field, selection.direction, selection.depth]);
  const onEdge = (id: string) => dispatch({ type: 'edge', id });
  const onField = (id: string) => dispatch({ type: 'field', id, model });
  return <main>
    <header>
      <div className="brand"><div className="brand-mark" aria-hidden="true">⌘</div><div><span className="eyebrow">CONSTRAINIUM / LOCAL EXPLORER</span><h1>Service dependency graph</h1><p>Explore services. Inspect schemas. Trace the data.</p></div></div>
      <div className="header-center">
        <div className="dataset" aria-label="Loaded graph summary">
          <span className="filename" title={filename}>◈ {filename}</span>
          <div className="dataset-stats"><span>{model.services.length} services</span><span>{model.edges.length} edges</span><span>{model.fields.size} fields</span><span>{model.connections.length} connections</span></div>
        </div>
        <HighlightControls selection={selection} dispatch={dispatch}/>
      </div>
      <FileLoader onLoad={(m, name) => { setModel(m); setFilename(name); dispatch({ type: 'reset' }); }}/>
    </header>
    <ResizableLayout graph={<GraphCanvas model={model} selection={selection} highlight={highlight} onSelect={onEdge}/>} details={<EdgeDetails model={model} selection={selection} related={highlight.fields} onEdge={onEdge} onField={onField}/>} connections={<ConnectionsPanel model={model} active={selection.field} highlight={highlight} onField={onField}/>}/>
    <footer>GraphJsonExporter v1 <span>Explicit mappings only · All data stays in this browser</span></footer>
  </main>;
}
