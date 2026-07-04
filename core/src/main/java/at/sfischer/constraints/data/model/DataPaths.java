package at.sfischer.constraints.data.model;

import java.util.List;

public class DataPaths {

    private final List<DataPathNode> roots;

    protected DataPaths(List<DataPathNode> roots) {
        this.roots = roots;
    }

    public List<DataPathNode> getRoots() {
        return roots;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roots.size(); i++) {
            append(sb, roots.get(i), "", i == roots.size() - 1);
        }
        return sb.toString();
    }

    private void append(StringBuilder sb, DataPathNode node, String prefix, boolean isLast) {
        sb.append(prefix)
                .append(isLast ? "└── " : "├── ")
                .append(format(node))
                .append(System.lineSeparator());

        List<DataPathNode> children = node.getNext();
        for (int i = 0; i < children.size(); i++) {
            append(
                    sb,
                    children.get(i),
                    prefix + (isLast ? "    " : "│   "),
                    i == children.size() - 1
            );
        }
    }

    private String format(DataPathNode node) {
        return node.getEdge() + " : " + node.getEntry().getQualifiedName();
    }
}
