import { z } from 'zod';
export type ExportType = { kind: 'ANY' | 'INTEGER' | 'NUMBER' | 'BOOLEAN' | 'STRING' | 'COMPLEXTYPE' } | { kind: 'ARRAY'; elementType: ExportType };
const type: z.ZodType<ExportType> = z.lazy(() => z.union([
  z.object({ kind: z.enum(['ANY', 'INTEGER', 'NUMBER', 'BOOLEAN', 'STRING', 'COMPLEXTYPE']) }),
  z.object({ kind: z.literal('ARRAY'), elementType: type }),
]));
const ref = z.string().min(1);
const schema = z.discriminatedUnion('kind', [
  z.object({ id: ref, kind: z.literal('simple'), entries: z.array(ref), parentEntry: ref.nullable() }),
  z.object({ id: ref, kind: z.literal('input-output'), input: ref.nullable(), output: ref.nullable(), inputPrefix: z.string(), outputPrefix: z.string(), parentEntry: ref.nullable() }),
]);
export const documentSchema = z.object({
  format: z.literal('constrainium-call-graph'), version: z.literal(1), entryNodes: z.array(ref),
  nodes: z.array(z.object({ id: ref, name: z.string(), type: z.string().default('unknown'), incoming: z.array(ref), outgoing: z.array(ref) })),
  edges: z.array(z.object({ id: ref, kind: z.enum(['dependency', 'synchronous-call', 'asynchronous-call', 'database-query']), from: ref, to: ref, schema: ref.nullable().optional(), query: z.string().nullable().optional(), dataFlows: z.array(ref).optional() })),
  schemas: z.array(schema),
  schemaEntries: z.array(z.object({ id: ref, name: z.string(), qualifiedName: z.string(), type, mandatory: z.boolean(), parentSchema: ref.nullable(), nestedSchema: ref.nullable(), constraints: z.array(z.unknown()), potentialConstraints: z.array(z.unknown()) })),
  dataFlows: z.array(z.object({ id: ref, traceId: z.string().nullable().optional(), from: ref, to: ref, mappings: z.array(z.object({ from: ref, to: ref })) })),
  objects: z.array(z.object({ id: ref, javaType: z.string() }).passthrough()),
});
export type ExportDocument = z.infer<typeof documentSchema>;
