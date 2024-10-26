package at.sfischer.constraints.data;

import at.sfischer.constraints.model.Node;

import java.util.Map;

public interface DataEntryVisitor<T> {

    void visitDataValues(Map<String, Node> values, T dataEntry);
}
