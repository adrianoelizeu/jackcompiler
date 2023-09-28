package br.ufma.ecp.token;

public class Token {

    public final TokenType type;
    public final String lexeme;
    final int line;

    public Token(TokenType type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }

    public String toString() {
        String categoria = type.toString().toLowerCase();

        String valor = lexeme;
        if (TokenType.isSymbol(lexeme.charAt(0))) {
            categoria = "symbol";
            // Os símbolos <, >, ", e & são impressos como &lt;  &gt;  &quot; e &amp; Para não conflitar com o significado destes símbolos no XML
            if (valor.equals(">")) {
                valor = "&gt;";
            } else if (valor.equals("<")) {
                valor = "&lt;";
            } else if (valor.equals("\"")) {
                valor = "&quot;";
            } else if (valor.equals("&")) {
                valor = "&amp;";
            }

        } else if (categoria.equals("number")) {
            categoria = "integerConstant";
        } else if (categoria.equals("ident")) {
            categoria = "identifier";
        } else if (categoria.equals("string")) {
            categoria = "stringConstant";
        } else if (categoria.equals("comma")) {
            categoria = "symbol";
        } else if (categoria.equals("semicolon")) {
            categoria = "symbol";
        } else if (categoria.equals("dot")) {
            categoria = "symbol";
        } else if (categoria.equals("plus")) {
            categoria = "symbol";
        } else if (categoria.equals("minus")) {
            categoria = "symbol";
        } else if (categoria.equals("asterisk")) {
            categoria = "symbol";
        } else if (categoria.equals("slash")) {
            categoria = "symbol";
        } else if (categoria.equals("and")) {
            categoria = "symbol";
        } else if (categoria.equals("or")) {
            categoria = "symbol";
        } else if (categoria.equals("not")) {
            categoria = "symbol";
        } else if (categoria.equals("lt")) {
            categoria = "symbol";
        } else if (categoria.equals("gt")) {
            categoria = "symbol";
        } else if (categoria.equals("eq")) {
            categoria = "symbol";
        } else if (categoria.equals("lparen")) {
            categoria = "symbol";
        } else if (categoria.equals("rparen")) {
            categoria = "symbol";
        } else if (categoria.equals("lbrace")) {
            categoria = "symbol";
        } else if (categoria.equals("rbrace")) {
            categoria = "symbol";
        } else if (categoria.equals("lbracket")) {
            categoria = "symbol";
        } else if (categoria.equals("rbracket")) {
            categoria = "symbol";
        } else {
            categoria = "keyword";
        }
        return "<" + categoria + "> " + valor + " </" + categoria + ">";
    }

}
