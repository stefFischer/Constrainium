# Graph JSON snapshots (version 1)

Export a graph as formatted JSON without running inference or changing the graph:

```java
import at.sfischer.constraints.data.model.utils.GraphJsonExporter;
import java.nio.file.Path;

String json = GraphJsonExporter.export(graph);
GraphJsonExporter.write(graph, Path.of("graph.json")); // UTF-8
```

This is a serialization for inspecting and processing graphs, including in
non-Java tools. A Java deserializer and a visualization are not included.

The root contains `format: "constrainium-call-graph"`, `version: 1` and these tables:

| Property | Contents |
| --- | --- |
| `entryNodes` | IDs of graph entry nodes |
| `nodes` | Original node `id`, `name`, and ordered `incoming`/`outgoing` edge IDs |
| `edges` | Generated `id`, `kind`, `from`/`to` node IDs; call edges also have `schema` and ordered `dataFlows` IDs |
| `schemas` | Simple schemas or input/output schemas, including custom prefixes and parent-entry references |
| `schemaEntries` | Field IDs, names, qualified names, types, mandatory flags, parent/nested schemas, confirmed and potential constraints |
| `dataFlows` | Flow ID, trace ID, source/target edge IDs, and exact source/target field-ID mappings |
| `objects` | Referenced constraint, expression, literal, provenance and retention-policy state |

## References and identity

IDs are unique within their table. Original graph node IDs are preserved.
Other IDs are local to the snapshot and must not be used as stable identifiers
across snapshots. Node rows are ordered by ID; field names are sorted; adjacency
and flow-ownership lists retain their original order. This is not a canonical
serialization across independently constructed graphs.

Cycles and parallel edges are supported. Two distinct field or schema instances
remain distinct even if their names and contents are equal. Shared instances
have a single table row. This matters for `DataFlow`'s identity-based mappings.

The `dataFlows` list on an edge records ownership. A flow's `from` and `to`
record its direction, which can differ from the owning edge. Each mapping
references entries in `schemaEntries`; matching fields by name is unnecessary.
Incoming lists are included explicitly so consumers do not have to reconstruct
adjacency or assume its ordering.

## Edges

Supported kinds are `synchronous-call`, `asynchronous-call`, `database-query`,
and `dependency`. Database-query edges also contain the exact `query` string.
Synchronous/database edges reference input/output schemas; asynchronous edges
reference simple schemas. A null schema is represented explicitly as null.

## Schemas

Simple schemas contain an `entries` array of field IDs. Input/output schemas
contain `input` and `output` schema IDs, plus `inputPrefix` and `outputPrefix`.
Every schema includes `parentEntry`, which may be null. Synthetic parent entries
created for input/output schemas are also serialized.

Every field includes `parentSchema` and `nestedSchema` (nullable) references.
Primitive types use `{"kind":"STRING"}` (or `ANY`, `INTEGER`, `NUMBER`,
`BOOLEAN`, `COMPLEXTYPE`). Arrays use recursive type descriptions:

```json
{"kind":"ARRAY","elementType":{"kind":"STRING"}}
```

`constraints` and `potentialConstraints` contain object references such as
`{"$ref":"object-0"}`. Empty schemas, empty constraint sets, optional fields,
and disconnected nodes are retained.

## Constraint state

Constraint state is structured data, not a printed expression. An object row
has an `id`, a `javaType`, and either:

- `fields`: a map from `declaring.class.Name#fieldName` to serialized values;
- `items`: array/collection elements; or
- `entries`: map entries containing `key` and `value`.

Fields include inherited, nonstatic state. For example, a `Constraint` refers
to its `term` and `derivedFrom`, and expression objects refer to their operands.
Shared objects and cycles use `$ref`. No expression is evaluated and no computed
bean getters are invoked. These implementation-state objects are intended for
inspection; graph/schema tables form the simpler interface for visualization.

Strings, booleans and null are inline. Numbers in this object-state table use
`{"javaType":"java.lang.Long","value":"9007199254740993"}` to preserve
precision in JavaScript and allow non-finite floating values. Characters and
enums are similarly tagged. Types, schemas and schema entries use explicit
type descriptions, `schemaRef`, and `fieldRef` respectively. Numeric metadata
in the graph tables, such as the format version, remains JSON numbers.

## Completeness and validation

The exporter handles the current built-in graph, schema, and type classes, and
constraint-state classes under `at.sfischer.constraints`. It rejects unsupported
subclasses rather than silently dropping their additional state. An external
custom constraint or policy requires explicit serializer support.

Edges must reference the registered node instances. Flow endpoints must be
edges in the graph; mapped fields must belong to its serialized edge schemas.
Invalid references produce `IllegalArgumentException` instead of dangling IDs.
Do not mutate a graph while exporting it.

The snapshot includes only state stored in `Graph`. It cannot contain original
span payload samples or log events unless those are added to the graph model.
