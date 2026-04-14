package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.Span;

import java.util.ArrayList;
import java.util.List;

public class TraceDiffer {

    public static List<Difference> diff(Span reference, Span compared, SpanComparator comparator) {
        List<Difference> differences = new ArrayList<>();
        diffRecursive(reference, compared, comparator, differences, "");
        return differences;
    }

    private static void diffRecursive(Span ref, Span cmp, SpanComparator comparator, List<Difference> diffs, String path) {
        if(ref == cmp){
            return;
        }

        String currentPath = path + "/" + (ref != null ? ref.getName() : cmp.getName());
        if (ref == null) {
            diffs.add(new Difference(Difference.Type.ADDED, null, cmp, currentPath + " was added"));
            return;
        }

        if (cmp == null) {
            diffs.add(new Difference(Difference.Type.REMOVED, ref, null, currentPath + " was removed"));
            return;
        }

        if (!comparator.isSame(ref, cmp)) {
            diffs.add(new Difference(Difference.Type.CHANGED, ref, cmp, currentPath + " was changed"));
        }

        // match children: simple version using index
        List<Span> refChildren = ref.getChildren();
        List<Span> cmpChildren = cmp.getChildren();

        int max = Math.max(refChildren.size(), cmpChildren.size());
        for (int i = 0; i < max; i++) {
            Span rChild = i < refChildren.size() ? refChildren.get(i) : null;
            Span cChild = i < cmpChildren.size() ? cmpChildren.get(i) : null;
            diffRecursive(rChild, cChild, comparator, diffs, currentPath);
        }
    }
}
