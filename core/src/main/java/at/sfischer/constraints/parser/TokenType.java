package at.sfischer.constraints.parser;

public enum TokenType {

    // ===== Keywords =====
    POLICY,
    GROUP,
    CONSTRAINT,
    FORALL,
    EXISTS,
    AND,
    OR,
    TRUE,
    FALSE,
    ARRAY_ELEMENT,

    // ===== constraint retention policies =====
    NO_VIOLATIONS,
    MIN_APPLICATIONS,

    // ===== Identifiers & Literals =====
    IDENTIFIER,
    NUMBER,
    STRING,

    // ===== Operators =====
    PLUS,           // +
    MINUS,          // -
    STAR,           // *
    SLASH,          // /
    MODULO,         // %
    POWER,          // ^

    EQUAL_EQUAL,    // ==
    BANG_EQUAL,     // !=
    LESS,           // <
    LESS_EQUAL,     // <=
    GREATER,        // >
    GREATER_EQUAL,  // >=

    AND_AND,        // &&
    OR_OR,          // ||

    BANG,           // !

    ASSIGN,         // =

    // ===== Delimiters =====
    LEFT_PAREN,     // (
    RIGHT_PAREN,    // )
    LEFT_BRACE,     // {
    RIGHT_BRACE,    // }
    LEFT_BRACKET,   // [
    RIGHT_BRACKET,  // ]
    COLON,          // :
    COMMA,          // ,

    EOF
}
