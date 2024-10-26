package at.sfischer.constraints.data;

import at.sfischer.constraints.model.Literal;

import java.util.Map;

public interface DataEntryVisitor<T> {

    void visitDataValues(Map<String, Literal<?>> values, T dataEntry);
}
