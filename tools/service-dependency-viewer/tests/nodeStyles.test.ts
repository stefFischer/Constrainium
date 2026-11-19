import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, renderHook, waitFor, act } from '@testing-library/react';
import sample from '../public/sample.json';
import { parseExport } from '../src/data/adapter';
import { builtInStyles, parseStyles, removeTypeStyle, serializeStyles, setTypeStyle, styleForType, styleStorageKey, validateStyles } from '../src/styles/nodeStyles';
import { useNodeStyles } from '../src/styles/useNodeStyles';

afterEach(() => { cleanup(); localStorage.clear(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });
describe('node types and style configuration', () => {
  it('reads the exporter type and supports old exports with unknown types', () => {
    expect(parseExport(JSON.stringify(sample)).services.find(s => s.id === 'database')?.type).toBe('database');
    const old = structuredClone(sample); old.nodes.forEach(n => Reflect.deleteProperty(n, 'type'));
    expect(parseExport(JSON.stringify(old)).services.every(s => s.type === 'unknown')).toBe(true);
    expect(() => parseExport(JSON.stringify({ ...sample, nodes: sample.nodes.map(n => ({ ...n, type: 23 })) }))).toThrow('type');
  });
  it('keeps the original default and inherits unspecified properties', () => {
    expect(builtInStyles.default).toEqual({ shape: 'round-rectangle', fill: '#e7effa', border: '#bdcde2', text: '#132c48', width: 128, height: 46, fontSize: 13, borderWidth: 1 });
    const config = setTypeStyle(builtInStyles, 'database', { shape: 'ellipse', fill: '#ff0000' });
    expect(styleForType(config, 'database')).toEqual({ ...builtInStyles.default, shape: 'ellipse', fill: '#ff0000' });
    expect(styleForType(config, 'new-type')).toEqual(builtInStyles.default);
    expect(styleForType(setTypeStyle(config, null, { width: 200 }), 'database').width).toBe(200);
    expect(styleForType(removeTypeStyle(config, 'database'), 'database')).toEqual(builtInStyles.default);
  });
  it('round-trips styles for types absent from the current graph', () => {
    const config = setTypeStyle(builtInStyles, 'some-future-type', { borderWidth: 3 });
    expect(parseStyles(serializeStyles(config))).toEqual(config);
  });
  it('matches literal type names without selectors or inherited object properties', () => {
    const name = 'db["\\]';
    const config = setTypeStyle(builtInStyles, name, { shape: 'diamond' });
    expect(styleForType(config, name).shape).toBe('diamond');
    for (const type of ['constructor', '__proto__']) {
      expect(styleForType(config, type)).toEqual(builtInStyles.default);
      const changed = setTypeStyle(config, type, { width: 180 });
      expect(styleForType(parseStyles(serializeStyles(changed)), type).width).toBe(180);
    }
  });
  it.each([
    { ...builtInStyles, version: 2 },
    { ...builtInStyles, default: { ...builtInStyles.default, fill: 'url(https://example.com/image)' } },
    { ...builtInStyles, default: { ...builtInStyles.default, width: -1 } },
    { ...builtInStyles, types: { db: { shape: 'unsupported' } } },
    { ...builtInStyles, types: { db: { opacity: 0 } } },
    { ...builtInStyles, default: {} }, { ...builtInStyles, types: [] },
  ])('rejects invalid styles instead of applying arbitrary Cytoscape properties', config => {
    expect(() => validateStyles(config)).toThrow();
  });
  it('reports malformed JSON', () => { expect(() => parseStyles('{')).toThrow('Invalid style JSON'); });
});

describe('style startup and persistence', () => {
  const remote = setTypeStyle(builtInStyles, null, { fill: '#aabbcc' });
  function startup(text = serializeStyles(remote)) { vi.stubGlobal('fetch', vi.fn(async () => new Response(text))); }
  it('loads startup JSON, then saved browser styles; reset restores startup and removes saved settings', async () => {
    startup(); const saved = setTypeStyle(remote, 'service', { shape: 'hexagon' });
    localStorage.setItem(styleStorageKey, serializeStyles(saved));
    const { result } = renderHook(useNodeStyles);
    await waitFor(() => expect(result.current.ready).toBe(true));
    expect(result.current.config).toEqual(saved);
    act(() => result.current.reset());
    expect(result.current.config).toEqual(remote);
    expect(localStorage.getItem(styleStorageKey)).toBeNull();
  });
  it('persists applied styles and leaves startup settings unchanged', async () => {
    startup(); const { result } = renderHook(useNodeStyles);
    await waitFor(() => expect(result.current.ready).toBe(true));
    const config = setTypeStyle(remote, 'db', { shape: 'ellipse' });
    act(() => result.current.save(config));
    expect(parseStyles(localStorage.getItem(styleStorageKey)!)).toEqual(config);
    act(() => result.current.reset()); expect(result.current.config).toEqual(remote);
  });
  it('falls back to the original style if startup JSON is invalid', async () => {
    startup('{}'); const { result } = renderHook(useNodeStyles);
    await waitFor(() => expect(result.current.ready).toBe(true));
    expect(result.current.config).toEqual(builtInStyles);
    expect(result.current.notice).toContain('Startup styles could not be loaded');
  });
  it('ignores malformed saved styles and reports the problem', async () => {
    startup(); localStorage.setItem(styleStorageKey, '{');
    const { result } = renderHook(useNodeStyles);
    await waitFor(() => expect(result.current.ready).toBe(true));
    expect(result.current.config).toEqual(remote);
    expect(result.current.notice).toContain('Saved browser styles');
  });
  it('still applies styles when browser storage cannot be written', async () => {
    startup(); const { result } = renderHook(useNodeStyles);
    await waitFor(() => expect(result.current.ready).toBe(true));
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('quota'); });
    act(() => result.current.save(builtInStyles));
    expect(result.current.config).toEqual(builtInStyles);
    expect(result.current.notice).toContain('Save JSON');
  });
});
