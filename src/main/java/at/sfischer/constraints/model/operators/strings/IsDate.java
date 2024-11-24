package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.*;
import at.sfischer.constraints.model.operators.Function;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class IsDate extends Function {

    private static final String FUNCTION_NAME = "string.isDate";

    public static final StringLiteral[] DATE_PATTERNS = new StringLiteral[]{
            new StringLiteral("yyyy-MM-dd"),
            new StringLiteral("yyyy/MM/dd")
    };

    public IsDate(Node value) {
        this(value, new ArrayValues<>(TypeEnum.STRING, DATE_PATTERNS));
    }

    public IsDate(Node value, ArrayValues<StringLiteral> patterns) {
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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            LocalDate.parse(dateString, formatter);
            return true;
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return false;
        }
    }

    @Override
    public Node setVariableValues(Map<Variable, Node> values) {
        //noinspection unchecked
        return new IsDate(getParameter(0).setVariableValues(values), (ArrayValues<StringLiteral>) getParameter(1));
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
