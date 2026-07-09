package at.sfischer.constraints.parser;

import at.sfischer.constraints.model.Node;

import java.io.IOException;

public interface ExtensionParserContext {
    Token current();
    Token previous();
    boolean check(TokenKind type);
    boolean match(TokenKind... types) throws IOException;
    Token consume(TokenKind type, String message) throws IOException, ParseException;
    void advance() throws IOException;
    Node parseExpression() throws IOException, ParseException;
}