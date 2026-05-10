package at.sfischer.traces.otel.prune;

import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.differ.Difference;
import at.sfischer.traces.otel.differ.SpanComparator;
import at.sfischer.traces.otel.differ.TraceDiffer;

import java.util.ArrayList;
import java.util.List;

public class DuplicateSubTracePruner implements TracePruner<Span> {

    private final SpanComparator comparator;

    public DuplicateSubTracePruner(SpanComparator comparator) {
        this.comparator = comparator;
    }

    @Override
    public void prune(Span root) {
        pruneRecursive(root);
    }

    private void pruneRecursive(Span span) {
        for (Span child : span.getChildren()) {
            pruneRecursive(child);
        }

        List<Span> children = span.getChildren();
        List<Span> uniqueLeaves = new ArrayList<>();
        List<Span> keptChildren = new ArrayList<>();
        for (Span child : children) {
            boolean duplicate = false;
            for (Span existing : uniqueLeaves) {
                List<Difference> diffs = TraceDiffer.diff(existing,child, comparator);
                if (diffs.isEmpty()) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) {
                uniqueLeaves.add(child);
                keptChildren.add(child);
            }
        }

        children.clear();
        children.addAll(keptChildren);
    }
}
