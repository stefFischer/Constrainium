package at.sfischer.constraints.parser;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.TypeEnum;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.Function;
import at.sfischer.constraints.parser.registry.FunctionCreateException;
import at.sfischer.constraints.parser.registry.FunctionCreator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UserFunctionCreator implements FunctionCreator {
    private final Node node;
    private final String name;
    private final List<String> parameters;

    public UserFunctionCreator(Node node, String name, List<String> parameters) {
        this.node = node;
        this.name = name;
        this.parameters = parameters;
    }

    public Function create(List<Node> arguments) throws FunctionCreateException {
        if(arguments.size() != parameters.size()){
            throw new FunctionCreateException("Number of arguments does not match the required parameters.");
        }
        Map<Variable, Type> variableTypeMap = this.node.inferVariableTypes();
        Map<String, Type> variableNameTypeMap = new HashMap<>();
        variableTypeMap.forEach((k,v) -> variableNameTypeMap.put(k.getName(), v));
        List<Type> parameterTypes = new LinkedList<>();
        for (String parameter : parameters) {
            Type parameterType = variableNameTypeMap.get(parameter);
            if(parameterType == null){
                parameterTypes.add(TypeEnum.ANY);
            } else {
                parameterTypes.add(parameterType);
            }
        }

        return new UserFunction(name, arguments, parameters, parameterTypes, node);
    }

    public Node getNode() {
        return node;
    }

    public String getName() {
        return name;
    }

    public List<String> getParameters() {
        return parameters;
    }
}
