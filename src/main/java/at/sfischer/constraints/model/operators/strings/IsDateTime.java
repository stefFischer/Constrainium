package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class IsDateTime extends Function {

    private static final String FUNCTION_NAME = "string.isDateTime";

    public static final StringLiteral[] DATE_PATTERNS = new StringLiteral[]{
            new StringLiteral("yyyy-MM-dd'T'HH:mm:ss"),
            new StringLiteral("yyyy-MM-dd'T'HH:mm:ss.SSS")
    };

    public IsDateTime(Node value) {
        this(value, new ArrayValues<>(TypeEnum.STRING, DATE_PATTERNS));
    }

    public IsDateTime(Node value, ArrayValues<StringLiteral> patterns) {
        super(FUNCTION_NAME, value, patterns);
    }

    @Override
    public Node evaluate() {
        String first = this.getStringArgument(0);
        String[] patterns = this.getStringArrayArgument(1);

        if(first != null && patterns != null){
            for (String pattern : patterns) {
                if(isDateTime(first, pattern)){
                    return new BooleanLiteral(true);
                }
            }

            return new BooleanLiteral(false);
        }

        return this;
    }

    static boolean isDateTime(String dateString, String pattern){
        try{
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            LocalDateTime.parse(dateString, formatter);
            return true;
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return false;
        }
    }

    @Override
    public Node setVariableValues(Map<Variable, Literal<?>> values) {
        //noinspection unchecked
        return new IsDateTime(getParameter(0).setVariableValues(values), (ArrayValues<StringLiteral>) getParameter(1));
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
