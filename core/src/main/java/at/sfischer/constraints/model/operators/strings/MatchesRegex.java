package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class MatchesRegex extends Function {

    private static final String FUNCTION_NAME = "string.matchesRegex";

    public MatchesRegex(Node value, StringLiteral pattern) {
        this(FUNCTION_NAME, value, pattern);
    }

    protected MatchesRegex(String functionName, Node value, StringLiteral pattern) {
        super(functionName, value, pattern);
    }

    @Override
    public Node evaluate() {
        String value = this.getStringArgument(0);
        String regex = this.getStringArgument(1);

        if(value != null){
            return BooleanLiteral.getBooleanLiteral(
                Pattern.compile(regex)
                    .matcher(value)
                    .matches()
            );
        }

        return this;
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        return new MatchesRegex(getParameter(0).setVariableValues(values), (StringLiteral)getParameter(1));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0), getParameter(1));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.STRING, TypeEnum.STRING);
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }
}
