import cytoscape, { type Core } from 'cytoscape';
import type { Model } from '../model';
import type { Selection } from '../state';
import type { traceFields } from '../highlighting';
const nodeId = (id: string) => JSON.stringify(['node', id]);
const edgeId = (id: string) => JSON.stringify(['edge', id]);
export function createGraph(container: HTMLElement, model: Model, onSelect: (id: string) => void) {
  const cy = cytoscape({ container, minZoom: .15, maxZoom: 3, selectionType: 'single',
    elements: [
      ...model.services.map(n => ({ data: { id: nodeId(n.id), label: n.name || n.id } })),
      ...model.edges.map(e => ({ data: { id: edgeId(e.id), originalId: e.id, source: nodeId(e.source), target: nodeId(e.target), label: `${e.id} · ${e.type.replace('-call', '')}` } })),
    ],
    layout: { name: 'circle', padding: 35, radius: Math.max(110, model.services.length * 22) },
    style: [
      { selector: 'node', style: { label: 'data(label)', shape: 'round-rectangle', width: 128, height: 46, 'background-color': '#e7effa', color: '#132c48', 'font-size': 13, 'font-weight': 'bold', 'text-valign': 'center', 'text-wrap': 'wrap', 'text-max-width': '116px', 'border-width': 1, 'border-color': '#bdcde2' } },
      { selector: 'edge', style: { label: 'data(label)', width: 2, 'curve-style': 'bezier', 'control-point-step-size': 65, 'target-arrow-shape': 'triangle', 'line-color': '#8b9eb7', 'target-arrow-color': '#8b9eb7', 'font-size': 10, color: '#405470', 'text-rotation': 'autorotate', 'text-background-color': '#f7f9fc', 'text-background-opacity': 1, 'text-background-padding': '3px' } },
      { selector: '.dim', style: { opacity: .18 } },
      { selector: 'edge.related', style: { width: 4, 'line-color': '#187f83', 'target-arrow-color': '#187f83', 'line-style': 'dashed', color: '#145e61' } },
      { selector: 'edge.active', style: { width: 5, 'line-style': 'solid', 'line-color': '#4257d6', 'target-arrow-color': '#4257d6', color: '#2438b3', 'font-weight': 'bold' } },
    ],
  });
  const tap = (event: cytoscape.EventObject) => onSelect(event.target.data('originalId'));
  cy.on('tap', 'edge', tap);
  const observer = new ResizeObserver(() => cy.resize()); observer.observe(container);
  return { cy, destroy() { observer.disconnect(); cy.off('tap', 'edge', tap); cy.destroy(); } };
}
export function updateGraph(cy: Core, selection: Selection, highlight: ReturnType<typeof traceFields>) {
  cy.batch(() => {
    cy.elements().removeClass('active related dim');
    if (selection.field) {
      cy.nodes().forEach(n => { if (!highlight.services.has(JSON.parse(n.id())[1])) n.addClass('dim'); });
      cy.edges().forEach(e => { e.addClass(highlight.edges.has(e.data('originalId')) ? 'related' : 'dim'); });
    }
    if (selection.edge) cy.getElementById(edgeId(selection.edge)).removeClass('dim').addClass('active');
  });
}
