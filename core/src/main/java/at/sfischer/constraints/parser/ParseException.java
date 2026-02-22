package at.sfischer.constraints.parser;

public class ParseException extends Exception {

    private final int line;
    private final int column;
    private final Token token;

    public ParseException(String message, Token token) {
        super(buildMessage(message, token));
        this.token = token;
        this.line = token.getLine();
        this.column = token.getColumn();
    }

    private static String buildMessage(String message, Token token) {
        return String.format(
                "Parse error at line %d, column %d near '%s': %s",
                token.getLine(),
                token.getColumn(),
                token.getLexeme(),
                message
        );
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public Token getToken() {
        return token;
    }
}
