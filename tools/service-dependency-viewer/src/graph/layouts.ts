import cytoscape, { type Core, type LayoutOptions } from 'cytoscape';
import dagre from 'cytoscape-dagre';

cytoscape.use(dagre);

export const layouts = [
  { id: 'hierarchy-lr', label: 'Hierarchy →', description: 'Left-to-right dependencies; feedback edges can point backwards.' },
  { id: 'hierarchy-tb', label: 'Hierarchy ↓', description: 'Top-to-bottom dependencies; feedback edges can point upwards.' },
  { id: 'force', label: 'Force-directed', description: 'Groups connected services and separates disconnected components.' },
  { id: 'circle', label: 'Circle', description: 'Places all services around a circle.' },
  { id: 'grid', label: 'Grid', description: 'Places services in evenly spaced rows and columns.' },
] as const;
export type LayoutId = typeof layouts[number]['id'];
export const defaultLayout: LayoutId = 'hierarchy-lr';

export function layoutOptions(id: LayoutId): LayoutOptions | dagre.DagreLayoutOptions {
  const common = { fit: true, padding: 35, animate: false, nodeDimensionsIncludeLabels: true };
  switch (id) {
    case 'hierarchy-lr':
    case 'hierarchy-tb': return { ...common, name: 'dagre', rankDir: id === 'hierarchy-lr' ? 'LR' : 'TB', nodeSep: 65, rankSep: 110, edgeSep: 25, acyclicer: 'greedy' };
    case 'force': return { ...common, name: 'cose', randomize: true, componentSpacing: 100, nodeRepulsion: () => 12000, idealEdgeLength: () => 140, numIter: 1000 };
    case 'circle': return { ...common, name: 'circle', avoidOverlap: true, spacingFactor: 1.15 };
    case 'grid': return { ...common, name: 'grid', avoidOverlap: true, avoidOverlapPadding: 55, condense: true };
  }
}

// Run on the existing instance and all elements, including those dimmed by field tracing.
export function runLayout(cy: Core, id: LayoutId) {
  if (cy.nodes().empty()) return;
  const layout = cy.layout(layoutOptions(id));
  layout.run();
  return layout;
}
