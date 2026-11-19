import type { Core } from 'cytoscape';
import { styleForType, type NodeStyle, type NodeStyleConfig } from '../styles/nodeStyles';

export function nodeStyleData(style: NodeStyle) {
  return { styleShape: style.shape, styleFill: style.fill, styleBorder: style.border, styleText: style.text,
    styleWidth: style.width, styleHeight: style.height, styleFontSize: style.fontSize,
    styleBorderWidth: style.borderWidth, styleTextWidth: style.width - 12 };
}
export function applyNodeStyles(cy: Core, config: NodeStyleConfig) {
  cy.batch(() => { cy.nodes().forEach(n => { n.data(nodeStyleData(styleForType(config, n.data('nodeType')))); }); });
}
