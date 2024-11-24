package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class IsHour extends Function {

    private static final String FUNCTION_NAME = "string.IsHour";

    public static final StringLiteral[] HOUR_PATTERNS_24H = new StringLiteral[]{
            new StringLiteral("HH:mm"),
    };

    public static final StringLiteral[] HOUR_PATTERNS_12H = new StringLiteral[]{
            new StringLiteral("hh:mm a")
    };

    public static final StringLiteral[] HOUR_PATTERNS_24H_S = new StringLiteral[]{
            new StringLiteral("HH:mm:ss")
    };

    public IsHour(Node value, ArrayValues<StringLiteral> patterns) {
        super(FUNCTION_NAME, value, patterns);
    }

    @Override
    public Node evaluate() {
        String first = this.getStringArgument(0);
        String[] patterns = this.getStringArrayArgument(1);

        if(first != null && patterns != null){
            for (String pattern : patterns) {
                if(isDate(first, pattern)){
                    return BooleanLiteral.TRUE;
                }
            }

            return BooleanLiteral.FALSE;
        }

        return this;
    }

    static boolean isDate(String dateString, String pattern){
        try{
            SimpleDateFormat parseFormat = new SimpleDateFormat(pattern);
            parseFormat.parse(dateString);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        //noinspection unchecked
        return new IsHour(getParameter(0).setVariableValues(values), (ArrayValues<StringLiteral>) getParameter(1));
    }

    @Override
    public List<Node> getChildren() {
        return List.of(getParameter(0));
    }

    @Override
    public List<Type> parameterTypes() {
        return List.of(TypeEnum.STRING, new ArrayType(TypeEnum.STRING));
    }

    @Override
    public Type getReturnType() {
        return TypeEnum.BOOLEAN;
    }
}
