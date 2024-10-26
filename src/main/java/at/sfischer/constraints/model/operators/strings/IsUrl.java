package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class IsUrl extends Function {

    private static final String FUNCTION_NAME = "string.isUrl";

    public IsUrl(Node value) {
        super(FUNCTION_NAME, value);
    }

    @Override
    public Node evaluate() {
        String first = this.getStringArgument(0);

        if(first != null){
            try{
                new URL(first);
                return new BooleanLiteral(true);
            } catch (MalformedURLException e) {
                return new BooleanLiteral(false);
            }
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new IsUrl(getParameter(0).setVariableValues(values));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.STRING);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }
}
