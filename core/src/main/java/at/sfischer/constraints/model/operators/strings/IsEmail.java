package at.sfischer.constraints.model.operators.strings;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.StringLiteral;

public class IsEmail extends MatchesRegex {

    private static final String FUNCTION_NAME = "string.isEmail";

    private static final StringLiteral PATTERN = new StringLiteral("^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");

    public IsEmail(Node value) {
        super(FUNCTION_NAME, value, PATTERN);
    }
}
