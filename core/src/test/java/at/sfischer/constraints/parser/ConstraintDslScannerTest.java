package at.sfischer.constraints.parser;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintDslScannerTest {

    private List<Token> scan(String input) throws Exception {
        StringReader reader = new StringReader(input);
        ConstraintDslScanner scanner = new ConstraintDslScanner(reader);
        return scanner.scanTokens();
    }

    private void assertToken(Token token, TokenType type, String lexeme) {
        assertEquals(type, token.getType());
        assertEquals(lexeme, token.getLexeme());
    }

    // =====================================
    // Keywords
    // =====================================

    @Test
    void testKeywords() throws Exception {
        List<Token> tokens = scan("policy group constraint forall exists true false");

        assertToken(tokens.get(0), TokenType.POLICY, "policy");
        assertToken(tokens.get(1), TokenType.GROUP, "group");
        assertToken(tokens.get(2), TokenType.CONSTRAINT, "constraint");
        assertToken(tokens.get(3), TokenType.FORALL, "forall");
        assertToken(tokens.get(4), TokenType.EXISTS, "exists");
        assertToken(tokens.get(5), TokenType.TRUE, "true");
        assertToken(tokens.get(6), TokenType.FALSE, "false");
        assertEquals(TokenType.EOF, tokens.get(7).getType());
    }

    // =====================================
    // Identifiers
    // =====================================

    @Test
    void testIdentifiers() throws Exception {
        List<Token> tokens = scan("myPolicy another_group x1 _tempVar");

        assertToken(tokens.get(0), TokenType.IDENTIFIER, "myPolicy");
        assertToken(tokens.get(1), TokenType.IDENTIFIER, "another_group");
        assertToken(tokens.get(2), TokenType.IDENTIFIER, "x1");
        assertToken(tokens.get(3), TokenType.IDENTIFIER, "_tempVar");
    }

    // =====================================
    // Numbers
    // =====================================

    @Test
    void testIntegerAndDecimalNumbers() throws Exception {
        List<Token> tokens = scan("42 3.14");

        assertToken(tokens.get(0), TokenType.NUMBER, "42");
        assertToken(tokens.get(1), TokenType.NUMBER, "3.14");
    }

    // =====================================
    // Strings
    // =====================================

    @Test
    void testStringLiteral() throws Exception {
        List<Token> tokens = scan("\"hello world\"");

        assertToken(tokens.get(0), TokenType.STRING, "hello world");
    }

    @Test
    void testStringLiteralWithEscapedQuote() throws Exception {
        String input = "\"\\\"hello world\\\"\"";
        List<Token> tokens = scan(input);

        assertToken(tokens.get(0), TokenType.STRING, "\"hello world\"");
    }

    // =====================================
    // Operators
    // =====================================

    @Test
    void testOperators() throws Exception {
        List<Token> tokens = scan("== != <= >= < > && || + - * / ! =");

        assertToken(tokens.get(0), TokenType.EQUAL_EQUAL, "==");
        assertToken(tokens.get(1), TokenType.BANG_EQUAL, "!=");
        assertToken(tokens.get(2), TokenType.LESS_EQUAL, "<=");
        assertToken(tokens.get(3), TokenType.GREATER_EQUAL, ">=");
        assertToken(tokens.get(4), TokenType.LESS, "<");
        assertToken(tokens.get(5), TokenType.GREATER, ">");
        assertToken(tokens.get(6), TokenType.AND_AND, "&&");
        assertToken(tokens.get(7), TokenType.OR_OR, "||");
        assertToken(tokens.get(8), TokenType.PLUS, "+");
        assertToken(tokens.get(9), TokenType.MINUS, "-");
        assertToken(tokens.get(10), TokenType.STAR, "*");
        assertToken(tokens.get(11), TokenType.SLASH, "/");
        assertToken(tokens.get(12), TokenType.BANG, "!");
        assertToken(tokens.get(13), TokenType.ASSIGN, "=");
    }

    // =====================================
    // Delimiters
    // =====================================

    @Test
    void testDelimiters() throws Exception {
        List<Token> tokens = scan("( ) { } : ,");

        assertToken(tokens.get(0), TokenType.LEFT_PAREN, "(");
        assertToken(tokens.get(1), TokenType.RIGHT_PAREN, ")");
        assertToken(tokens.get(2), TokenType.LEFT_BRACE, "{");
        assertToken(tokens.get(3), TokenType.RIGHT_BRACE, "}");
        assertToken(tokens.get(4), TokenType.COLON, ":");
        assertToken(tokens.get(5), TokenType.COMMA, ",");
    }

    // =====================================
    // Quantifier Expression
    // =====================================

    @Test
    void testQuantifierSnippet() throws Exception {
        List<Token> tokens = scan("forall a: a <= 5");

        assertToken(tokens.get(0), TokenType.FORALL, "forall");
        assertToken(tokens.get(1), TokenType.IDENTIFIER, "a");
        assertToken(tokens.get(2), TokenType.COLON, ":");
        assertToken(tokens.get(3), TokenType.IDENTIFIER, "a");
        assertToken(tokens.get(4), TokenType.LESS_EQUAL, "<=");
        assertToken(tokens.get(5), TokenType.NUMBER, "5");
    }

    // =====================================
    // Full DSL Snippet
    // =====================================

    @Test
    void testFullPolicyAndGroupSnippet() throws Exception {

        String input = """
                policy STRICT {
                    noViolations
                    minApplications = 3
                }

                group numeric {
                    policy = STRICT
                    constraint less:
                        a <= b
                }
                """;

        List<Token> tokens = scan(input);

        assertEquals(TokenType.POLICY, tokens.get(0).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType());
        assertEquals(TokenType.LEFT_BRACE, tokens.get(2).getType());

        // just ensure no crash and EOF present
        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).getType());
    }

    // =====================================
    // Error Handling
    // =====================================

    @Test
    void testUnexpectedCharacterThrows() {
        Exception exception = assertThrows(
                RuntimeException.class,
                () -> scan("@")
        );

        assertTrue(exception.getMessage().contains("Unexpected character"));
    }

    @Test
    void testUnterminatedStringThrows() {
        Exception exception = assertThrows(
                RuntimeException.class,
                () -> scan("\"unterminated")
        );

        assertTrue(exception.getMessage().contains("Unterminated string"));
    }
}

