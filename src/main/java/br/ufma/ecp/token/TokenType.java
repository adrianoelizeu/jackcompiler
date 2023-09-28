package br.ufma.ecp.token;

import java.util.List;
//import java.util.Map; --não utilizado

public enum TokenType {
    // Literals.
    NUMBER,
    IDENT,
    STRING,

    // keywords
    METHOD, WHILE, IF, CLASS, CONSTRUCTOR,
    FUNCTION, FIELD, STATIC, VAR, INT,
    CHAR, BOOLEAN, VOID, TRUE, FALSE,
    NULL, THIS, LET, DO, ELSE, RETURN,

    // symbols
    LPAREN, RPAREN,
    LBRACE, RBRACE,
    LBRACKET, RBRACKET,

    COMMA, SEMICOLON, DOT,

    PLUS, MINUS, ASTERISK, SLASH,

    AND, OR, NOT,

    LT, GT, EQ,

    EOF,

    ILLEGAL;

    static public boolean isSymbol(char c) {
        String symbols = "{}()[].,;+-*/&|<>=~";
        return symbols.indexOf(c) > -1;
    }

    static public boolean isKeyword(TokenType type) {
        List<TokenType> keywords = List.of(
            METHOD, WHILE, IF, CLASS, CONSTRUCTOR,
            FUNCTION, FIELD, STATIC, VAR, INT,
            CHAR, BOOLEAN, VOID, TRUE, FALSE,
            NULL, THIS, LET, DO, ELSE, RETURN);
        return keywords.contains(type);
    }
}
