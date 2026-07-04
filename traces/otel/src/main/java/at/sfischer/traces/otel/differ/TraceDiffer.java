package at.sfischer.traces.otel.differ;

import at.sfischer.traces.otel.TraceNode;

import java.util.ArrayList;
import java.util.List;

public class TraceDiffer {

    public static <T extends TraceNode<T>> List<Difference<T>> diff(T reference, T compared, SpanComparator<T> comparator) {
        List<Difference<T>> differences = new ArrayList<>();
        diffRecursive(reference, compared, comparator, differences, "");
        return differences;
    }

    private static <T extends TraceNode<T>> void diffRecursive(T ref, T cmp, SpanComparator<T> comparator, List<Difference<T>> diffs, String path) {
        if(ref == cmp){
            return;
        }

        String currentPath = path + "/" + (ref != null ? ref.getName() : cmp.getName());
        if (ref == null) {
            diffs.add(new Difference<T>(Difference.Type.ADDED, null, cmp, currentPath + " was added"));
            return;
        }

        if (cmp == null) {
            diffs.add(new Difference<T>(Difference.Type.REMOVED, ref, null, currentPath + " was removed"));
            return;
        }

        if (!comparator.isSame(ref, cmp)) {
            diffs.add(new Difference<T>(Difference.Type.CHANGED, ref, cmp, currentPath + " was changed"));
        }

        // match children: simple version using index
        List<T> refChildren = ref.getChildren();
        List<T> cmpChildren = cmp.getChildren();

        int max = Math.max(refChildren.size(), cmpChildren.size());
        for (int i = 0; i < max; i++) {
            T rChild = i < refChildren.size() ? refChildren.get(i) : null;
            T cChild = i < cmpChildren.size() ? cmpChildren.get(i) : null;
            diffRecursive(rChild, cChild, comparator, diffs, currentPath);
        }
    }
}
