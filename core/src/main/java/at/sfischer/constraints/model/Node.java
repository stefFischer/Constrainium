package at.sfischer.constraints.model;

import at.sfischer.constraints.model.operators.Operator;

import java.util.*;

public interface Node {

    Type getReturnType();

    /**
     *
     * @return the result of the evaluation.
     */
    Node evaluate();

    boolean validate();

    List<Node> getChildren();

    default Node setVariableNameValue(String name, Node value){
        Map<Variable, Node> variableValues = new HashMap<>();
        variableValues.put(new Variable(name), value);
        return setVariableValues(variableValues);
    }

    default Node setVariableNameValues(Map<String, Node> values){
        Map<Variable, Node> variableValues = new HashMap<>();
        for (Map.Entry<String, Node> entry : values.entrySet()) {
            variableValues.put(new Variable(entry.getKey()), entry.getValue());
        }

        return setVariableValues(variableValues);
    }

    default Node setVariableValue(Variable variable, Node value){
        Map<Variable, Node> variableValues = new HashMap<>();
        variableValues.put(variable, value);
        return setVariableValues(variableValues);
    }

    Node setVariableValues(Map<Variable, Node> values);

    default Node cloneNode(){
        return setVariableValues(new HashMap<>());
    }

    default String nodeString() {
        return nodeString("");
    }

    default String nodeString(String indent) {
        List<Node>  children = getChildren();
        StringBuilder s = new StringBuilder(indent);
        s.append(this);
        if(children != null) {
            List<Type> types = null;
            if(this instanceof Operator){
                types = ((Operator) this).operandTypes();
            }
            int i = 0;
            for (Node child : children) {
                s.append("\n");
                s.append(child.nodeString(indent + "\t"));
                if(types != null && child instanceof Variable){
                    s.append(" Type: ");
                    s.append(types.get(i));
                }
                i++;
            }
        }

        return s.toString();
    }

    /**
     * Visits node in a depth-first search manner.
     *
     * @param visitor
     */
    default void visitNodes(NodeVisitor visitor){
        boolean visitChildren = visitor.visitNode(this);
        if(visitChildren) {
            List<Node> children = getChildren();
            if (children != null) {
                for (Node child : children) {
                    child.visitNodes(visitor);
                }
            }
        }
    }

    default Map<Variable, Type> inferVariableTypes(){
        return inferVariableTypes(new HashSet<>());
    }

    default Map<Variable, Type> inferVariableTypes(Node... childrenToExclude){
        return inferVariableTypes(new HashSet<>(Arrays.asList(childrenToExclude)));
    }

    default Map<Variable, Type> inferVariableTypes(Set<Node> childrenToExclude){
        Map<Variable, Type> variableTypes = new HashMap<>();
        List<Node>  children = getChildren();
        if(children != null) {
            if (!(this instanceof Operator)){
                return variableTypes;
            }

            List<Type> types = ((Operator) this).operandTypes();
            if(types == null){
                return variableTypes;
            }

            int i = 0;
            for (Node child : children) {
                if(childrenToExclude.contains(child)){
                    continue;
                }

                if (child instanceof Variable){
                    Type variableType = types.get(i);
                    Type t = variableTypes.get((Variable)child);
                    if(t != null && !t.equals(variableType)){
                        throw new IllegalStateException("Variable " + child + " has inconsistent types: " + t + " and " + variableType);
                    }

                    if(t == null){
                        variableTypes.put((Variable)child, variableType);
                    }
                }
                i++;

                Map<Variable, Type> childTypes = child.inferVariableTypes();
                for (Map.Entry<Variable, Type> childEntry : childTypes.entrySet()) {
                    Type t = variableTypes.get(childEntry.getKey());
                    if(t == null){
                        variableTypes.put(childEntry.getKey(), childEntry.getValue());
                        continue;
                    }

                    if(!t.equals(childEntry.getValue())){
                        throw new IllegalStateException("Variable " + childEntry.getKey() + " has inconsistent types: " + t + " and " + childEntry.getValue());
                    }
                }
            }
        }

        return variableTypes;
    }
}
