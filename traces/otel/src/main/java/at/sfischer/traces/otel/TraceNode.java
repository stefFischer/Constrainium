package at.sfischer.traces.otel;

import at.sfischer.traces.otel.matching.MatchResult;
import at.sfischer.traces.otel.matching.TraceNodeMatch;

import java.util.*;

public abstract class TraceNode<T extends TraceNode<T>> {

    protected final String name;

    protected final String kind;

    protected final Attributes attributes;

    protected final List<T> children;

    protected TraceNode<T> parent;

    protected TraceNode(String name, String kind) {
        this.name = name;
        this.kind = kind;
        this.attributes = new Attributes();
        this.children = new LinkedList<>();
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public <V> V getAttribute(String key) {
        return attributes.get(key);
    }

    public <V> V removeAttribute(String key) {
        return attributes.remove(key);
    }

    public void putAttributes(Attributes attributes) {
        this.attributes.putAll(attributes);
    }

    public TraceNode<T> getParent() {
        return parent;
    }

    public List<T> getChildren() {
        return List.copyOf(children);
    }

    public void addChild(T child) {
        children.add(child);
        child.parent = this;
    }

    public static <T extends TraceNode<T>> T find(Collection<T> collection, TraceNodeMatch<T> match){
        Optional<T> found = collection.stream()
                .filter(s -> match.matches(s) == MatchResult.SUCCESS)
                .findFirst();
        return found.orElse(null);
    }

    public T findChild(TraceNodeMatch<T> match){
        return TraceNode.find(children, match);
    }

    public T findSibling(TraceNodeMatch<T> match){
        if(parent == null){
            return null;
        }

        List<T> siblings = this.parent.getChildren()
                .stream()
                .filter(s -> s != this)
                .toList();
        return TraceNode.find(siblings, match);
    }

    public void removeChildren(Collection<T> toRemove){
        children.removeAll(toRemove);
    }

    public void retainChildren(Collection<T> toRetain){
        children.retainAll(toRetain);
    }

    public void visit(TraceNodeVisitor<T> visitor) {
        boolean visitChildren = visitor.visit(thisAsT());
        if (visitChildren) {
            children.forEach(child -> child.visit(visitor));
        }
    }

    public int countSpans() {
        final int[] count = {0};
        visit(node -> {
            count[0]++;
            return true;
        });

        return count[0];
    }

    public int getDepth() {
        if (children.isEmpty()) {
            return 1;
        }

        int maxChildDepth = 0;
        for (T child : children) {
            int childDepth = child.getDepth();
            if (childDepth > maxChildDepth) {
                maxChildDepth = childDepth;
            }
        }

        return 1 + maxChildDepth;
    }

    public int getBreadth() {
        Map<Integer, Integer> levelCounts = new HashMap<>();
        computeBreadth(0, levelCounts);

        int maxBreadth = 0;
        for (int count : levelCounts.values()) {
            if (count > maxBreadth) {
                maxBreadth = count;
            }
        }

        return maxBreadth;
    }

    protected void computeBreadth(int level, Map<Integer, Integer> levelCounts) {
        levelCounts.put(level, levelCounts.getOrDefault(level, 0) + 1);
        for (T child : children) {
            child.computeBreadth(level + 1, levelCounts);
        }
    }

    @SuppressWarnings("unchecked")
    private T thisAsT() {
        return (T) this;
    }

    public String toStringWithChildren() {
        return toStringWithChildren("");
    }

    protected String toStringWithChildren(String indent) {
        StringBuilder sb = new StringBuilder(indent);
        sb.append(this);
        for (T child : children) {
            sb.append("\n");
            sb.append(child.toStringWithChildren(indent + "\t"));
        }

        return sb.toString();
    }
}
