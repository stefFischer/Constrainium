import { expect, test } from '@playwright/test';
import sample from '../public/sample.json' with { type: 'json' };
test('static build loads, navigates, resizes and replaces snapshots locally', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', e => errors.push(e.message));
  await page.goto('/');
  await page.getByRole('button', { name: 'Load sample' }).click();
  await expect(page.getByText('6 services', { exact: true })).toBeVisible();
  await expect(page.locator('.graph canvas').first()).toBeVisible();
  // Cytoscape's container registry is used only to observe the actual rendered camera in this test.
  const camera = () => page.locator('.graph').evaluate(el => {
    const cy = (el as unknown as { _cyreg: { cy: import('cytoscape').Core } })._cyreg.cy;
    return { zoom: cy.zoom(), pan: cy.pan(), positions: cy.nodes().map(n => n.position()) };
  });
  const before = await camera();
  const connections = page.getByRole('region', { name: 'Field connections' });
  await connections.getByRole('button').filter({ hasText: 'rows[]' }).first().click();
  const active = page.getByRole('button', { pressed: true });
  await expect(active).toContainText('id');
  await expect(active).toBeFocused();
  expect(await camera()).toEqual(before);
  await expect(page.getByRole('button', { name: 'Collapse rows' })).toBeVisible();
  await page.getByLabel('Transitive', { exact: true }).check();
  await page.getByLabel('Downstream', { exact: true }).check();
  const divider = page.getByRole('separator', { name: 'Resize graph and details' });
  await divider.focus(); await divider.press('ArrowLeft');
  await expect(divider).toHaveAttribute('aria-valuenow', '58');
  await page.getByRole('button', { name: 'Fit view' }).click();
  await page.getByLabel('Open GraphJsonExporter JSON file').setInputFiles({ name: 'bad.json', mimeType: 'application/json', buffer: Buffer.from('{"version":99}') });
  await expect(page.getByRole('alert')).toContainText('version');
  await expect(page.getByText('6 services', { exact: true })).toBeVisible();
  await page.getByLabel('Open GraphJsonExporter JSON file').setInputFiles({ name: 'replacement.json', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(sample)) });
  await expect(page.getByRole('button', { pressed: true })).toHaveCount(0);
  await expect(page.getByLabel('Both directions')).toBeChecked();
  await expect(page.getByLabel('Direct', { exact: true })).toBeChecked();
  await expect(page.getByRole('alert')).toHaveCount(0);
  await connections.getByRole('button').filter({ hasText: 'rows[]' }).first().click();
  await expect(page.getByRole('button', { pressed: true })).toBeFocused();
  await expect(page.getByRole('button', { pressed: true })).toBeInViewport();
  await page.screenshot({ path: 'test-results/viewer.png', fullPage: true });
  expect(errors).toEqual([]);
});

test('layout changes preserve tracing and handle cycles, parallel edges and disconnected services', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', e => errors.push(e.message));
  await page.goto('/');
  await page.getByRole('button', { name: 'Load sample' }).click();
  const picker = page.getByRole('combobox', { name: 'Graph layout' });
  await expect(picker).toHaveValue('hierarchy-lr');
  await page.getByRole('region', { name: 'Field connections' }).getByRole('button').filter({ hasText: 'rows[]' }).first().click();
  const activeId = await page.getByRole('button', { pressed: true }).getAttribute('data-field');
  const selectedEdge = await page.getByLabel('Select an edge').inputValue();
  const graphState = () => page.locator('.graph').evaluate(el => {
    const cy = (el as unknown as { _cyreg: { cy: import('cytoscape').Core } })._cyreg.cy;
    return { positions: cy.nodes().map(n => n.position()), nodes: cy.nodes().length, edges: cy.edges().length, selected: cy.edges('.active').map(e => e.data('originalId')), related: cy.edges('.related').length };
  });
  for (const id of ['hierarchy-tb', 'force', 'circle', 'grid', 'hierarchy-lr']) {
    const previous = await graphState();
    await picker.selectOption(id);
    const current = await graphState();
    expect(current.positions).not.toEqual(previous.positions);
    expect(current.nodes).toBe(6); expect(current.edges).toBe(7);
    expect(current.positions.every(p => Number.isFinite(p.x) && Number.isFinite(p.y))).toBe(true);
    expect(current.selected).toEqual([selectedEdge]); expect(current.related).toBeGreaterThan(0);
    await expect(page.getByRole('button', { pressed: true })).toHaveAttribute('data-field', activeId!);
    await expect(page.getByLabel('Select an edge')).toHaveValue(selectedEdge);
  }
  await picker.selectOption('grid');
  const arranged = await graphState();
  await page.locator('.graph').evaluate(el => {
    const cy = (el as unknown as { _cyreg: { cy: import('cytoscape').Core } })._cyreg.cy;
    cy.nodes().first().position({ x: 2500, y: 2500 });
  });
  await page.getByRole('button', { name: 'Apply again' }).click();
  expect((await graphState()).positions).toEqual(arranged.positions);
  await page.getByRole('button', { name: 'Clear selection' }).click();
  expect((await graphState()).positions).toEqual(arranged.positions);
  // Replacing a graph keeps the layout preference and safely handles empty snapshots.
  await page.getByLabel('Open GraphJsonExporter JSON file').setInputFiles({ name: 'empty.json', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify({ format: sample.format, version: 1, entryNodes: [], nodes: [], edges: [], schemas: [], schemaEntries: [], dataFlows: [], objects: [] })) });
  await expect(picker).toHaveValue('grid');
  await expect(page.getByRole('button', { name: 'Apply again' })).toBeDisabled();
  await page.getByRole('button', { name: 'Load sample' }).click();
  await expect(picker).toHaveValue('grid');
  await expect.poll(async () => (await graphState()).nodes).toBe(6);
  await picker.selectOption('hierarchy-lr');
  await page.screenshot({ path: 'test-results/layouts.png', fullPage: true });
  expect(errors).toEqual([]);
});
