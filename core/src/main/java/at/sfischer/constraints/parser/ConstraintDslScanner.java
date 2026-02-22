package at.sfischer.constraints.parser;

import at.sfischer.constraints.model.operators.array.ArrayQuantifier;

import java.io.*;
import java.util.*;

public class ConstraintDslScanner {

    private final Reader reader;
    private final List<Token> tokens = new ArrayList<>();

    private int currentChar;
    private int nextChar;

    private int line = 1;
    private int column = 0;

    private static final Map<String, TokenType> keywords = new HashMap<>();

    static {
        keywords.putAll(Map.of(
                "policy", TokenType.POLICY,
                "group", TokenType.GROUP,
                "constraint", TokenType.CONSTRAINT,
                "forall", TokenType.FORALL,
                "exists", TokenType.EXISTS,
                "AND", TokenType.AND,
                "OR", TokenType.OR,
                "true", TokenType.TRUE,
                "false", TokenType.FALSE
        ));

        keywords.put(ArrayQuantifier.ELEMENT_NAME, TokenType.ARRAY_ELEMENT);

        // Policies
        keywords.putAll(Map.of(
                "noViolations", TokenType.NO_VIOLATIONS,
                "minApplications", TokenType.MIN_APPLICATIONS
        ));
    }

    // =============================
    // Constructors
    // =============================

    public ConstraintDslScanner(InputStream inputStream) throws IOException {
        this(new InputStreamReader(inputStream));
    }

    public ConstraintDslScanner(Reader reader) throws IOException {
        this.reader = new BufferedReader(reader);
        advanceNext();  // initialize nextChar
        advance();      // initialize currentChar
    }

    // =============================
    // Public API
    // =============================

    public List<Token> scanTokens() throws IOException {
        while (!isAtEnd()) {
            scanToken();
        }

        if(!lastTokenIsEOF()) {
            tokens.add(new Token(TokenType.EOF, "", line, column));
        }
        return tokens;
    }

    public Token nextToken() throws IOException {
        if(isAtEnd()){
            if(!lastTokenIsEOF()) {
                tokens.add(new Token(TokenType.EOF, "", line, column));
            }
        } else {
            scanToken();
            if(isAtEnd()){
                tokens.add(new Token(TokenType.EOF, "", line, column));
            }
        }

        return this.tokens.getLast();
    }

    // =============================
    // Core Scanning
    // =============================

    private void scanToken() throws IOException {

        skipWhitespace();

        if (isAtEnd()) return;

        int startLine = line;
        int startColumn = column;

        char c = (char) currentChar;

        // Single character tokens
        switch (c) {
            case '(' -> addSimple(TokenType.LEFT_PAREN, startLine, startColumn);
            case ')' -> addSimple(TokenType.RIGHT_PAREN, startLine, startColumn);
            case '{' -> addSimple(TokenType.LEFT_BRACE, startLine, startColumn);
            case '}' -> addSimple(TokenType.RIGHT_BRACE, startLine, startColumn);
            case '[' -> addSimple(TokenType.LEFT_BRACKET, startLine, startColumn);
            case ']' -> addSimple(TokenType.RIGHT_BRACKET, startLine, startColumn);
            case ':' -> addSimple(TokenType.COLON, startLine, startColumn);
            case ',' -> addSimple(TokenType.COMMA, startLine, startColumn);
            case '+' -> addSimple(TokenType.PLUS, startLine, startColumn);
            case '-' -> addSimple(TokenType.MINUS, startLine, startColumn);
            case '*' -> addSimple(TokenType.STAR, startLine, startColumn);
            case '/' -> addSimple(TokenType.SLASH, startLine, startColumn);
            case '%' -> addSimple(TokenType.MODULO, startLine, startColumn);
            case '^' -> addSimple(TokenType.POWER, startLine, startColumn);

            case '!' -> {
                if (peek() == '=') {
                    advance();
                    addToken(TokenType.BANG_EQUAL, "!=", startLine, startColumn);
                } else {
                    addSimple(TokenType.BANG, startLine, startColumn);
                }
            }

            case '=' -> {
                if (peek() == '=') {
                    advance();
                    addToken(TokenType.EQUAL_EQUAL, "==", startLine, startColumn);
                } else {
                    addSimple(TokenType.ASSIGN, startLine, startColumn);
                }
            }

            case '<' -> {
                if (peek() == '=') {
                    advance();
                    addToken(TokenType.LESS_EQUAL, "<=", startLine, startColumn);
                } else {
                    addSimple(TokenType.LESS, startLine, startColumn);
                }
            }

            case '>' -> {
                if (peek() == '=') {
                    advance();
                    addToken(TokenType.GREATER_EQUAL, ">=", startLine, startColumn);
                } else {
                    addSimple(TokenType.GREATER, startLine, startColumn);
                }
            }

            case '&' -> {
                if (peek() == '&') {
                    advance();
                    addToken(TokenType.AND_AND, "&&", startLine, startColumn);
                } else {
                    error("Unexpected character '&'");
                }
            }

            case '|' -> {
                if (peek() == '|') {
                    advance();
                    addToken(TokenType.OR_OR, "||", startLine, startColumn);
                } else {
                    error("Unexpected character '|'");
                }
            }

            case '"' -> string(startLine, startColumn);

            default -> {
                if (Character.isDigit(c)) {
                    number(startLine, startColumn);
                } else if (isAlpha(c)) {
                    identifier(startLine, startColumn);
                } else {
                    error("Unexpected character: " + c);
                }
            }
        }

        advance();
    }

    // =============================
    // Token Types
    // =============================

    private void identifier(int startLine, int startColumn) throws IOException {
        StringBuilder builder = new StringBuilder();

        builder.append((char) currentChar);

        while (isAlphaNumeric(peek())) {
            advance();
            builder.append((char) currentChar);
        }

        String text = builder.toString();
        TokenType type = keywords.getOrDefault(text, TokenType.IDENTIFIER);

        addToken(type, text, startLine, startColumn);
    }

    private void number(int startLine, int startColumn) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append((char) currentChar);

        while (Character.isDigit(peek())) {
            advance();
            builder.append((char) currentChar);
        }

        if (peek() == '.') {
            advance();
            builder.append((char) currentChar);

            while (Character.isDigit(peek())) {
                advance();
                builder.append((char) currentChar);
            }
        }

        addToken(TokenType.NUMBER, builder.toString(), startLine, startColumn);
    }

    private void string(int startLine, int startColumn) throws IOException {
        StringBuilder builder = new StringBuilder();
        advance(); // skip opening quote

        while (!isAtEnd()) {
            if (currentChar == '"') {
                break; // closing quote
            }

            if (currentChar == '\\') {
                advance(); // skip '\'
                if (isAtEnd()) {
                    error("Unterminated escape sequence in string.");
                }

                switch (currentChar) {
                    case '"'  -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case 'n'  -> builder.append('\n');
                    case 't'  -> builder.append('\t');
                    case 'r'  -> builder.append('\r');
                    default   -> error("Invalid escape sequence: \\" + (char) currentChar);
                }
            } else {
                if (currentChar == '\n') {
                    line++;
                    column = 0;
                }
                builder.append((char) currentChar);
            }

            advance();
        }

        if (isAtEnd()) {
            error("Unterminated string.");
        }

        advance(); // consume closing quote
        addToken(TokenType.STRING, builder.toString(), startLine, startColumn);
    }

    // =============================
    // Utilities
    // =============================

    private void skipWhitespace() throws IOException {
        while (!isAtEnd()) {
            if (currentChar == ' ' || currentChar == '\r' || currentChar == '\t') {
                advance();
            } else if (currentChar == '\n') {
                line++;
                column = 0;
                advance();
            } else {
                break;
            }
        }
    }

    private void addSimple(TokenType type, int line, int column) {
        addToken(type, String.valueOf((char) currentChar), line, column);
    }

    private void addToken(TokenType type, String lexeme, int line, int column) {
        tokens.add(new Token(type, lexeme, line, column));
    }

    private void advance() throws IOException {
        currentChar = nextChar;
        nextChar = reader.read();
        column++;
    }

    private void advanceNext() throws IOException {
        nextChar = reader.read();
    }

    private int peek() {
        return nextChar;
    }

    private boolean isAtEnd() {
        return currentChar == -1;
    }

    private boolean lastTokenIsEOF(){
        return !this.tokens.isEmpty() && this.tokens.getLast().getType() == TokenType.EOF;
    }

    private boolean isAlpha(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isAlphaNumeric(int c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.';
    }

    private void error(String message) {
        throw new RuntimeException(
                "Scanner error at line " + line + ", column " + column + ": " + message
        );
    }
}
