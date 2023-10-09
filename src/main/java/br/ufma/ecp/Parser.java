package br.ufma.ecp;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();
    private String className;

    public Parser(byte[] input) {
        scan = new Scanner(input);
        nextToken();
    }

    public void nextToken() {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }

    public void parse() {

    }

    public String XMLOutput() {
        return xmlOutput.toString();
    }

    private void printNonTerminal(String nterminal) {
        xmlOutput.append(String.format("<%s>\r\n", nterminal));
    }

    boolean peekTokenIs(TokenType type) {
        return peekToken.type == type;
    }

    boolean currentTokenIs(TokenType type) {
        return currentToken.type == type;
    }

    private void expectPeek(TokenType... types) {
        for (TokenType type : types) {
            if (peekToken.type == type) {
                expectPeek(type);
                return;
            }
        }

        throw error(peekToken, "Expected a statement");

    }

    private void expectPeek(TokenType type) {
        if (peekToken.type == type) {
            nextToken();
            xmlOutput.append(String.format("%s\r\n", currentToken.toString()));
        } else {
            throw error(peekToken, "Expected " + type.name());
        }
    }

    private static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
    }

    private ParseError error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
        return new ParseError();
    }

    void parseTerm() {
        printNonTerminal("term");
        switch (peekToken.type) {
            case NUMBER:
                expectPeek(TokenType.NUMBER);
                break;
            case STRING:
                expectPeek(TokenType.STRING);
                break;
            case FALSE:
            case NULL:
            case TRUE:
                expectPeek(TokenType.FALSE, TokenType.NULL, TokenType.TRUE);
                break;
            case THIS:
                expectPeek(TokenType.THIS);
                break;
            case IDENT:
                expectPeek(TokenType.IDENT);
                break;
            default:
                throw error(peekToken, "term expected");
        }

        printNonTerminal("/term");
    }

    void expr() {
        number();
        oper();
    }

    void number() {
        System.out.println(currentToken.lexeme);
        match(TokenType.NUMBER);
    }

    private void match(TokenType t) {
        if (currentToken.type == t) {
            nextToken();
        } else {
            throw new Error("syntax error");
        }
    }

    void oper() {
        if (currentToken.type == TokenType.PLUS) {
            match(TokenType.PLUS);
            number();
            System.out.println("add");
            oper();
        } else if (currentToken.type == TokenType.MINUS) {
            match(TokenType.MINUS);
            number();
            System.out.println("sub");
            oper();
        } else if (currentToken.type == TokenType.EOF) {
            // vazio
        } else {
            throw new Error("syntax error");
        }
    }

    public String VMOutput() {
        return "";
    }

    static public boolean isOperator(String op) {
        return "+-*/<>=~&|".contains(op);
    }

      public void parseCLass() {
        printNonTerminal("class");

        expectPeek(TokenType.CLASS);
        expectPeek(TokenType.IDENT);
        className = currentToken.value();
        expectPeek(TokenType.LBRACE);

        while (peekTokenIs(TokenType.STATIC) || peekTokenIs(TokenType.FIELD)) {
            parseClassVarDec();
        }

        while (peekTokenIs(TokenType.FUNCTION) || peekTokenIs(TokenType.CONSTRUCTOR) || peekTokenIs(TokenType.METHOD)) {
            parseSubroutineDec();
        }

        expectPeek(TokenType.RBRACE);

        printNonTerminal("/class");
    }

    public void parseVarDec() {
        printNonTerminal("varDec");
        expectPeek(TokenType.VAR);
        expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        String type = currentToken.value();
        expectPeek(TokenType.IDENT);
        String name = currentToken.value();

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);

            name = currentToken.value();
        }

        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/varDec");
    }

    public void parseClassVarDec() {
        printNonTerminal("classVarDec");

        expectPeek(TokenType.STATIC, TokenType.FIELD);

        expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        String type = currentToken.value();

        expectPeek(TokenType.IDENT);
        String name = currentToken.value();

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);
            name = currentToken.value();
        }

        expectPeek(TokenType.SEMICOLON);
        ;

        printNonTerminal("/classVarDec");
    }

    public void parseSubroutineDec() {
        printNonTerminal("subroutineDec");

        expectPeek(TokenType.CONSTRUCTOR, TokenType.FUNCTION, TokenType.METHOD);
        var subroutineType = currentToken.type;

        expectPeek(TokenType.VOID, TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        expectPeek(TokenType.IDENT);

        var functionName = className + "." + currentToken.value();

        expectPeek(TokenType.LPAREN);
        parseParameterList();
        expectPeek(TokenType.RPAREN);
        parseSubroutineBody();
        printNonTerminal("/subroutineDec");
    }

    public void parseParameterList() {
        printNonTerminal("parameterList");

        if (peekTokenIs(TokenType.RPAREN)) {
            expectPeek(TokenType.VOID, TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
            String type = currentToken.value();

            expectPeek(TokenType.IDENT);
            String name = currentToken.value();

            while (peekTokenIs(TokenType.COMMA)) {
                expectPeek(TokenType.COMMA);
                expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
                type = currentToken.value();

                expectPeek(TokenType.IDENT);
                name = currentToken.value();
            }

        }

        printNonTerminal("/parameterList");
    }

    private void parseSubroutineBody(String functionName, TokenType subroutineType) {

        printNonTerminal("subroutineBody");

        expectPeek(TokenType.LBRACE);

        while (peekTokenIs(TokenType.VAR)) {
            parseVarDec();
        }

        parseStatements();
        expectPeek(TokenType.RBRACE);

        printNonTerminal("/subroutineBody");
    }

    public void parseStatements() {
        printNonTerminal("statements");
        while (peekToken.type == TokenType.WHILE ||
                peekToken.type == TokenType.IF ||
                peekToken.type == TokenType.LET ||
                peekToken.type == TokenType.DO ||
                peekToken.type == TokenType.RETURN) {
            parseStatement();
        }
        printNonTerminal("/statements");
    }

    void parseStatement() {
        switch (peekToken.type) {
            case LET:
                parseLet();
                break;
            case WHILE:
                parseWhile();
                break;
            case IF:
                parseIf();
                break;
            case RETURN:
                parseReturn();
                break;
            case DO:
                parseDo();
                break;
            default:
                throw error(peekToken, "Expected a statement");
        }
    }

    public void parseLet() {
        printNonTerminal("letStatement");
        expectPeek(TokenType.LET);
        expectPeek(TokenType.IDENT);

        if (peekTokenIs(TokenType.LBRACKET)) {
            expectPeek(TokenType.LBRACKET);
            parseExpression();
            expectPeek(TokenType.RBRACKET);
        }

        expectPeek(TokenType.EQ);
        parseExpression();
        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/letStatement");
    }

    public void parseWhile() {
        printNonTerminal("whileStatement");
        expectPeek(TokenType.WHILE);
        expectPeek(TokenType.LPAREN);
        parseExpression();
        expectPeek(TokenType.RPAREN);
        expectPeek(TokenType.LBRACE);
        parseStatements();
        expectPeek(TokenType.RBRACE);
        printNonTerminal("/whileStatement");
    }

    public void parseIf() {
        printNonTerminal("ifStatement");
        expectPeek(TokenType.IF);

        expectPeek(TokenType.LPAREN);
        parseExpression();
        expectPeek(TokenType.RPAREN);

        expectPeek(TokenType.LBRACE);
        parseStatements();
        expectPeek(TokenType.RBRACE);

        if (peekTokenIs(TokenType.ELSE)) {
            expectPeek(TokenType.ELSE);
            expectPeek(TokenType.LBRACE);
            parseStatements();
            expectPeek(TokenType.RBRACE);
        }
        printNonTerminal("/ifStatement");
    }

    public void parseReturn() {
        printNonTerminal("returnStatement");
        expectPeek(TokenType.RETURN);
        if (!peekTokenIs(TokenType.SEMICOLON)) {
            parseExpression();
        }
        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/returnStatement");
    }

    public void parseDo() {
        printNonTerminal("doStatement");
        expectPeek(TokenType.DO);
        expectPeek(TokenType.IDENT);
        parseSubroutineCall();
        expectPeek(TokenType.SEMICOLON);

        printNonTerminal("/doStatement");
    }

    private void parseExpression() {
    }

    private void parseSubroutineCall() {
    }


}