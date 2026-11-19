import { z } from 'zod';
import defaults from './builtin-node-styles.json';

export const shapes = ['round-rectangle', 'rectangle', 'ellipse', 'diamond', 'hexagon', 'round-hexagon'] as const;
const color = z.string().regex(/^#[0-9a-fA-F]{6}$/, 'Use a six-digit hex color, e.g. #e7effa');
export const nodeStyleSchema = z.object({
  shape: z.enum(shapes), fill: color, border: color, text: color,
  width: z.number().int().min(60).max(400), height: z.number().int().min(30).max(240),
  fontSize: z.number().int().min(10).max(32), borderWidth: z.number().min(0).max(8),
}).strict();
const configSchema = z.object({
  format: z.literal('constrainium-node-styles'), version: z.literal(1), default: nodeStyleSchema,
  // Entries preserve literal type names such as "__proto__" without prototype mutation.
  types: z.preprocess(value => value && typeof value === 'object' && !Array.isArray(value) ? Object.entries(value) : null,
    z.array(z.tuple([z.string(), nodeStyleSchema.partial()])).transform(entries => Object.fromEntries(entries))),
}).strict();
export type NodeStyle = z.infer<typeof nodeStyleSchema>;
export type NodeStyleConfig = z.infer<typeof configSchema>;
export const builtInStyles = configSchema.parse(defaults);
export const styleStorageKey = 'constrainium.node-styles.v1';

export function validateStyles(value: unknown): NodeStyleConfig {
  const result = configSchema.safeParse(value);
  if (!result.success) throw new Error(result.error.issues.slice(0, 10).map(i => `${i.path.join('.') || 'styles'}: ${i.message}`).join('\n'));
  return result.data;
}
export function parseStyles(text: string): NodeStyleConfig {
  let value: unknown;
  try { value = JSON.parse(text); } catch { throw new Error('Invalid style JSON. Load a saved node styles file.'); }
  return validateStyles(value);
}
export function styleForType(config: NodeStyleConfig, type: string): NodeStyle {
  return { ...config.default, ...(Object.hasOwn(config.types, type) ? config.types[type] : {}) };
}
export function setTypeStyle(config: NodeStyleConfig, type: string | null, style: Partial<NodeStyle>): NodeStyleConfig {
  if (type === null) return validateStyles({ ...config, default: { ...config.default, ...style } });
  return validateStyles({ ...config, types: { ...config.types, [type]: style } });
}
export function removeTypeStyle(config: NodeStyleConfig, type: string): NodeStyleConfig {
  const types = { ...config.types }; delete types[type]; return { ...config, types };
}
export function serializeStyles(config: NodeStyleConfig) { return JSON.stringify(validateStyles(config), null, 2) + '\n'; }
export function downloadStyles(config: NodeStyleConfig) {
  const url = URL.createObjectURL(new Blob([serializeStyles(config)], { type: 'application/json' }));
  const a = document.createElement('a'); a.href = url; a.download = 'node-styles.json'; a.click();
  setTimeout(() => URL.revokeObjectURL(url), 0);
}
