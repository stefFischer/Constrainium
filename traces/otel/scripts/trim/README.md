# trim_otel_traces

A utility script for trimming large OpenTelemetry trace files down to a small,
representative subset suitable for checking into version control or use as test fixtures.

## Why this exists

OTEL exporters can produce trace files that are hundreds of megabytes in size.
GitHub enforces a 100MB hard limit per file, and large files slow down tests significantly.
A naive truncation of the file produces orphaned spans — spans whose parent or sibling spans
were cut off — which breaks trace assembly. This script only writes complete traces, so the
output is always valid.

It also filters out traces that were captured during application startup or infrastructure
setup by keeping only traces whose root span is a SERVER span (`kind=2`). These are the
traces that represent real incoming requests and are the most useful for testing.

## Requirements

Python 3.9 or later. No external dependencies — only the standard library is used.

## Usage

```
python trim_otel_traces.py <input> <output> [--max-traces N] [--max-mb M]
```

### Arguments

| Argument | Description |
|---|---|
| `input` | Path to the input OTEL JSON file |
| `output` | Path to write the trimmed output file |
| `--max-traces N` | Keep at most N complete traces (default: 100) |
| `--max-mb M` | Stop adding traces once the output exceeds M megabytes |

`--max-traces` and `--max-mb` can be combined — whichever limit is hit first stops the output.

### Examples

Keep the first 200 SERVER-rooted traces:
```
python trim_otel_traces.py microshop-traces.json trimmed.json --max-traces 200
```

Cap output at 5MB regardless of trace count:
```
python trim_otel_traces.py microshop-traces.json trimmed.json --max-mb 5
```

Combine both limits:
```
python trim_otel_traces.py microshop-traces.json trimmed.json --max-traces 500 --max-mb 10
```

### Example output

```
Loading microshop-traces.json ...
Grouping spans by traceId ...
Found 3847 unique traces total.
Found 312 traces with a SERVER root span.
Writing 100 complete traces to trimmed.json ...
Done. Output size: 3.42 MB
```

## Input format

Both common OTEL JSON export formats are supported:

**Single JSON object** — the entire file is one object:
```json
{ "resourceSpans": [ ... ] }
```

**NDJSON** — one JSON object per line:
```json
{ "resourceSpans": [ ... ] }
{ "resourceSpans": [ ... ] }
```

## How it works

1. All spans are loaded and grouped by `traceId`.
2. Traces are filtered to those with a root span of `kind=2` (SERVER) and no `parentSpanId`.
3. Up to `--max-traces` qualifying traces are selected, stopping early if `--max-mb` is reached.
4. Selected spans are reconstructed back into the original `resourceSpans` / `scopeSpans`
   structure, merging spans from the same resource and scope together.
5. The output is written as a formatted JSON file.

The resource and scope metadata for each span is preserved exactly as in the input,
so the output is a valid OTEL resource spans document that any standard parser can read.
