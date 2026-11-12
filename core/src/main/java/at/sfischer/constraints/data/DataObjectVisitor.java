package at.sfischer.constraints.data;

public interface DataObjectVisitor<T> {

    void visitDataValues(DataObject dataObject, T dataEntry);
}
