package at.sfischer.constraints.data;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.Variable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DataCollection<T> {

    boolean applyDataToTerms(List<Node> terms, Map<Variable, Type> variableTypes);

    int numberOfDataEntries();

    void visitDataEntries(Set<String> fieldNames, DataEntryVisitor<T> visitor);

    void addDataEntry(T dataEntry);

    void removeDataEntry(T dataEntry);

    DataCollection<T> clone();
}
