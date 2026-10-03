package at.sfischer.constraints.parser;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.Value;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.Function;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UserFunction extends Function {

    private final List<Type> parameterTypes;

    private final Node node;

    public UserFunction(String name, List<Node> arguments, List<Type> parameterTypes, Node node) {
        super(name, arguments);
        this.parameterTypes = parameterTypes;
        this.node = node;
    }

    @Override
    public Node evaluate() {
        Node result = node.evaluate();
        if(result == node && !(result instanceof Value<?>)){
            return this;
        }

        return result;
    }

    @Override
    public Type getReturnType() {
        return this.node.getReturnType();
    }

    @Override
    public List<Type> parameterTypes() {
        return parameterTypes;
    }

    @Override
    public List<Node> getChildren() {
        return this.parameters;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        Node setNode = this.node.setVariableValues(values);
        List<Node> arguments = new LinkedList<>();
        for (Node parameter : this.parameters) {
            arguments.add(parameter.setVariableValues(values));
        }
        return new UserFunction(this.getName(), arguments, parameterTypes, setNode);
    }
}
