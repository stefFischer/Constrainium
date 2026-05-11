package at.sfischer.traces.otel.prune;

import at.sfischer.traces.otel.Span;
import at.sfischer.traces.otel.differ.SpanComparator;

import java.util.ArrayList;
import java.util.List;

public class DuplicateLeafPruner implements TracePruner<Span> {

    private final SpanComparator<Span> comparator;

    public DuplicateLeafPruner(SpanComparator<Span> comparator) {
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
            if (!child.getChildren().isEmpty()) {
                keptChildren.add(child);
                continue;
            }

            boolean duplicate = false;
            for (Span existing : uniqueLeaves) {
                if (comparator.isSame(existing, child)) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) {
                uniqueLeaves.add(child);
                keptChildren.add(child);
            }
        }

        span.retainChildren(keptChildren);
    }
}
