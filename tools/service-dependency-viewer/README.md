# Service dependency viewer

A static React/TypeScript application for `GraphJsonExporter` snapshots. Imported data is read in the browser, held in memory, and never uploaded or persisted. There is no backend, analytics, or external runtime asset dependency.

## Install and run

Requires Node.js 22.12+ (Node 24 also works) and npm. The Java repository has no existing Node package manager or frontend conventions, so this tool has its own package and lockfile.

```sh
cd tools/service-dependency-viewer
npm ci
npm run dev
```

Open the localhost URL printed by Vite. On Windows with PowerShell script execution disabled, use `npm.cmd` in place of `npm`.

```sh
npm test
npm run build
npm run preview
```

`npm run test:e2e` checks the production build in installed Google Chrome (run the build first). It covers sample loading, field reveal/focus, controls, resizing, invalid input and replacement reset. To use Playwright's bundled Chromium instead, remove `channel: 'chrome'` from `playwright.config.ts` and run `npx playwright install chromium`.

Serve `dist/` from any static HTTP server, including a subdirectory (assets use relative URLs). Opening `index.html` directly with `file://` is not supported. Runtime Java is unnecessary.

## Load and explore

Choose **Load sample** or **Open JSON** and select a UTF-8 snapshot exported with:

```java
GraphJsonExporter.write(graph, Path.of("graph.json"));
```

Files up to 25 MiB are accepted. Errors include table paths or offending reference IDs. Invalid loads retain the current graph; valid loads reset selections, controls, and graph layout. The sample includes all four edge types, parallel calls, a service self-loop, disconnected service, missing schema, custom side prefixes, nested object arrays, nested primitive arrays, and a field-flow cycle.

Select an edge on the canvas or via the keyboard accessible edge selector. Inspect metadata and separate input/output trees. Select any field to trace its explicit mappings; **Active** and **Related** labels distinguish results without relying on color. The bottom panel shows all mappings until a field is selected, then only the traversed mappings. Click either endpoint to select its edge, open the correct side, expand ancestors, scroll to and focus the field. Clear selection restores the graph appearance.

Direction defaults to **Both directions**, depth to **Direct**. Upstream follows mapping `to → from`; downstream follows `from → to`. Direct takes one hop. Transitive visits all reachable fields, with visited sets terminating cycles. Both is the union of independent upstream and downstream walks from the active field; it does not switch direction at each hop. Trace IDs are shown as provenance; traversal unions explicit mappings across traces and does not imply a single execution path.

Drag nodes to arrange them, drag the canvas to pan, scroll or use buttons to zoom, and use **Fit view** to fit. Selection changes preserve layout and viewport. Drag panel dividers to resize; focused dividers support arrow keys, Home and End. All actions have native keyboard controls. Schema expand buttons and headings toggle with Enter/Space; field buttons also support Left/Right to collapse/expand. Tab traverses visible fields and references.

The **Layout** selector rearranges all services and fits the graph, preserving the selected edge, active field, and highlights:

- **Hierarchy →** (default): Dagre arranges dependencies from left to right.
- **Hierarchy ↓**: the same arrangement from top to bottom.
- **Force-directed**: Cytoscape CoSE groups connected services using a spring simulation, useful for cyclic topologies.
- **Circle**: services around a circle.
- **Grid**: evenly spaced rows and columns.

Hierarchical layouts support cycles by drawing feedback edges against the main direction; edge directions and connections are unchanged. Parallel edges, self-loops and disconnected services remain present. **Apply again** restores the chosen layout after manual dragging (force-directed can produce a different arrangement). **Fit view** changes only the viewport. The chosen layout stays selected when loading another file; ordinary selection, tracing and panel resizing never rerun it. Layouts run locally without animation or additional network requests.

**Full screen**, at the far right of the graph controls, expands just the graph panel, including its layout and viewport controls. Use **Exit full screen** or the browser's Escape shortcut to return to the three-panel view. The canvas resizes automatically, preserving node positions, selection and highlighting; **Fit view** can fit the graph to the larger space. The button is disabled if the browser or embedding page does not support full screen, and rejected requests show an inline error.

## Export contract and assumptions

Inspected `core/.../data/model/Graph.java`, its edge and flow classes, `SimpleDataSchema`, `InOutputDataSchema`, `DataSchemaEntry`, `TypeEnum`, and `GraphJsonExporter` plus its tests before implementation. No existing exported fixture was found among tracked project resources. This viewer consumes **the existing format**, not a new graph format:

- `format: "constrainium-call-graph"`, `version: 1`; `entryNodes`, `nodes`, `edges`, `schemas`, `schemaEntries`, `dataFlows`, `objects` tables.
- Edges use `id`, `kind`, `from`, `to`, optional nullable `schema`/`query`, and `dataFlows` ownership references. Kinds are `dependency`, `synchronous-call`, `asynchronous-call`, `database-query`.
- Simple schemas list entry IDs; input-output schemas reference input/output schema IDs and custom prefixes. Entries include exact identity, name, qualified name, recursively typed arrays, mandatory flag, parent/nested schema references, and constraint metadata.
- Flows explicitly reference source/target edge IDs and mappings of exact entry IDs. Flow direction comes from the flow record, independently of the edge that owns it. Parent back-references are not field children.

Known gaps and deliberate assumptions:

1. Export-generated edge, schema, entry and flow IDs are stable within a snapshot, not guaranteed across exports. The viewer does not merge snapshots.
2. Mappings do not explicitly name a schema side or occurrence. The adapter resolves an exact entry ID within the stated edge's schema containment. A field shared across sides or paths makes the endpoint ambiguous; imports with such mappings report an error instead of inventing relationships. Shared schemas on different edges are supported.
3. Array entries describe collections, not concrete indexed elements. Paths preserve individual name segments and annotate ancestor array dimensions with `[]`. Identity uses a JSON tuple of edge ID, schema side, and exact entry ID chain, never a dotted name. Equal names, names containing punctuation, and equal paths on different sides cannot collide. Qualified names are metadata, not identity.
4. Synthetic input/output wrapper entries are represented by side headings. Mappings to wrappers, fields outside the stated edge, recursive containment, or nested input-output schemas cannot resolve to the supported unique field tree and produce actionable errors. The model's synchronous edges declare simple input/output schemas; nesting ordinary object schemas works. Schema depth is bounded at 100 to avoid recursive rendering exhaustion.
5. Missing schemas, empty schemas, absent queries and trace IDs are allowed. Constraint/provenance object state is accepted as opaque metadata; the viewer does not evaluate constraints or render object graphs. No relationships are inferred from service adjacency, field names, or constraint expressions.
6. Exporter metadata parent references are checked for referential integrity; rendering follows forward containment, since reused schema instances may have parent metadata from their most recent attachment.

## Modules

`src/data/exportFormat.ts` defines validated exporter types; `adapter.ts` handles reference checks and normalization. `model.ts` defines the internal graph. `state.ts` synchronizes selection and navigation; `highlighting.ts` performs pure traversal. `graph/cytoscape.ts` owns Cytoscape construction, styles, resize observation, and cleanup; `graph/layouts.ts` configures and runs the available layouts. Components handle file feedback, graph controls, details, schemas, connections and resizable panels independently. Only model replacement recreates Cytoscape; selection updates classes.

## Regenerate the sample

The checked-in `public/sample.json` is generated by `sample/GenerateSample.java` calling the actual `GraphJsonExporter`. From the repository root with JDK 21+ configured:

```sh
./gradlew -I tools/service-dependency-viewer/sample/generate.gradle :core:generateViewerSample
```

Use `gradlew.bat` on Windows. This task uses the core runtime classpath and leaves production Java sources unchanged.
