#!/usr/bin/env python3
"""
Trims an OpenTelemetry resource spans JSON file to the first N complete traces
that have a root span of kind SERVER (kind=2).

Usage:
    python trim_otel_traces.py <input.json> <output.json> [--max-traces N] [--max-mb M]
"""

import json
import argparse
import sys
from collections import defaultdict


def load_spans(path: str):
    """
    Handles two common OTEL export formats:
    - Single JSON object:  { "resourceSpans": [...] }
    - NDJSON:              one { "resourceSpans": [...] } object per line
    """
    with open(path, "r", encoding="utf-8") as f:
        first_char = f.read(1)
        f.seek(0)

        if first_char == "{":
            content = f.read()
            try:
                return json.loads(content)
            except json.JSONDecodeError:
                all_resource_spans = []
                for line in content.splitlines():
                    line = line.strip()
                    if not line:
                        continue
                    obj = json.loads(line)
                    all_resource_spans.extend(obj.get("resourceSpans", []))
                return {"resourceSpans": all_resource_spans}
        else:
            all_resource_spans = []
            for line in f:
                line = line.strip()
                if not line:
                    continue
                obj = json.loads(line)
                all_resource_spans.extend(obj.get("resourceSpans", []))
            return {"resourceSpans": all_resource_spans}


def group_by_trace(data: dict) -> dict[str, list]:
    """
    Returns a mapping of traceId -> list of (resourceSpans entry, scopeSpans entry, span).
    Preserves the full resource/scope context needed to reconstruct the output.
    """
    traces = defaultdict(list)
    for resource_span in data.get("resourceSpans", []):
        for scope_span in resource_span.get("scopeSpans", []):
            for span in scope_span.get("spans", []):
                trace_id = span.get("traceId")
                if trace_id:
                    traces[trace_id].append((resource_span, scope_span, span))
    return traces


def has_server_root(spans: list) -> bool:
    """
    Returns True if the trace has a root span (no parentSpanId) with kind=2 (SERVER).
    """
    for _, _, span in spans:
        parent_id = span.get("parentSpanId", "")
        kind = span.get("kind", 0)
        if (not parent_id) and kind == 2:
            return True
    return False


def build_output(selected_trace_ids: list,
                 traces: dict) -> dict:
    """
    Reconstructs a valid resourceSpans structure containing only the selected traces.
    Merges spans back under their original resource/scope to keep the format valid.
    """
    resource_map = defaultdict(lambda: defaultdict(list))
    resource_meta = {}
    scope_meta = {}

    for trace_id in selected_trace_ids:
        for resource_span, scope_span, span in traces[trace_id]:
            resource_key = json.dumps(
                resource_span.get("resource", {}), sort_keys=True)
            scope_key = json.dumps(
                scope_span.get("scope", {}), sort_keys=True)

            resource_meta[resource_key] = resource_span.get("resource", {})
            scope_meta[(resource_key, scope_key)] = scope_span.get("scope", {})
            resource_map[resource_key][scope_key].append(span)

    resource_spans_out = []
    for resource_key, scope_map in resource_map.items():
        scope_spans_out = []
        for scope_key, spans in scope_map.items():
            scope_spans_out.append({
                "scope": scope_meta[(resource_key, scope_key)],
                "spans": spans
            })
        resource_spans_out.append({
            "resource": resource_meta[resource_key],
            "scopeSpans": scope_spans_out
        })

    return {"resourceSpans": resource_spans_out}


def estimate_mb(data: dict) -> float:
    return len(json.dumps(data).encode("utf-8")) / (1024 * 1024)


def main():
    parser = argparse.ArgumentParser(
        description="Trim OTEL trace file to N complete SERVER-rooted traces.")
    parser.add_argument("input",  help="Input JSON file")
    parser.add_argument("output", help="Output JSON file")
    parser.add_argument("--max-traces", type=int, default=100,
                        help="Maximum number of complete traces to keep (default: 100)")
    parser.add_argument("--max-mb", type=float, default=None,
                        help="Stop adding traces once output exceeds this size in MB")
    args = parser.parse_args()

    print(f"Loading {args.input} ...", flush=True)
    data = load_spans(args.input)

    print("Grouping spans by traceId ...", flush=True)
    traces = group_by_trace(data)
    print(f"Found {len(traces)} unique traces total.")

    # Filter to only traces rooted at a SERVER span
    server_rooted = [tid for tid, spans in traces.items() if has_server_root(spans)]
    print(f"Found {len(server_rooted)} traces with a SERVER root span.")

    selected = []
    for trace_id in server_rooted[:args.max_traces]:
        selected.append(trace_id)
        if args.max_mb is not None:
            candidate = build_output(selected, traces)
            if estimate_mb(candidate) >= args.max_mb:
                print(f"Reached {args.max_mb}MB limit at {len(selected)} traces.")
                break

    print(f"Writing {len(selected)} complete traces to {args.output} ...")
    output = build_output(selected, traces)
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(output, f, indent=2)

    actual_mb = estimate_mb(output)
    print(f"Done. Output size: {actual_mb:.2f} MB")


if __name__ == "__main__":
    main()
