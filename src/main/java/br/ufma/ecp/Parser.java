package br.ufma.ecp;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

import br.ufma.ecp.SymbolTable.Kind;
import br.ufma.ecp.SymbolTable.Symbol;

import br.ufma.ecp.VMWriter.Command;
import br.ufma.ecp.VMWriter.Segment;

public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();

    private SymbolTable symbolTable;
    private SymbolTable symTable = new SymbolTable();

    private VMWriter vmWriter = new VMWriter();

    private String className; // nome da classe
    private int ifLabelNum = 0; // numero de if
    private int whileLabelNum = 0; // numero de while
    

    public Parser(byte[] input) {

        scan = new Scanner(input);
        symbolTable = new SymbolTable();
        vmWriter = new VMWriter();

        nextToken();
        ifLabelNum = 0;
        whileLabelNum = 0;
    }

    public void nextToken() {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }

    public void parse() {
        parseClass();
    }

    // 'class' className '{' classVarDec* subroutineDec* '}'
    public void parseClass() {

        printNonTerminal("class");
        expectPeek(TokenType.CLASS);
        expectPeek(TokenType.IDENT);
        className = currentToken.lexeme;
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

    // 'var' type varName ( ',' varName)* ';'
    void parseVarDec() {
        printNonTerminal("varDec");
        expectPeek(TokenType.VAR);

        SymbolTable.Kind kind = Kind.VAR;

        // 'int' | 'char' | 'boolean' | className
        expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        String type = currentToken.lexeme;

        expectPeek(TokenType.IDENT);
        String name = currentToken.lexeme;
        symTable.define(name, type, kind);

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);

            name = currentToken.lexeme;
            symTable.define(name, type, kind);

        }

        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/varDec");
    }

    // classVarDec → ( 'static' | 'field' ) type varName ( ',' varName)* ';'
    void parseClassVarDec() {
        printNonTerminal("classVarDec");
        expectPeek(TokenType.FIELD, TokenType.STATIC);

        SymbolTable.Kind kind = Kind.STATIC;
        if (currentTokenIs(TokenType.FIELD))
            kind = Kind.FIELD;

        // 'int' | 'char' | 'boolean' | className
        expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        String type = currentToken.lexeme;

        expectPeek(TokenType.IDENT);
        String name = currentToken.lexeme;

        symTable.define(name, type, kind);
        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);

            name = currentToken.lexeme;
            symTable.define(name, type, kind);
        }

        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/classVarDec");
    }

    // subroutineCall -> subroutineName '(' expressionList ')' | (className|varName)
    // '.' subroutineName '(' expressionList ')
    void parseSubroutineCall() {

        if (peekTokenIs(TokenType.LPAREN)) {
            expectPeek(TokenType.LPAREN);
            parseExpressionList();
            expectPeek(TokenType.RPAREN);
        } else {
            expectPeek(TokenType.DOT);
            expectPeek(TokenType.IDENT);

            expectPeek(TokenType.LPAREN);
            parseExpressionList();

            expectPeek(TokenType.RPAREN);
        }
    }

    // ( 'constructor' | 'function' | 'method' ) ( 'void' | type) subroutineName
    // '(' parameterList ')' subroutineBody
    void parseSubroutineDec() {

        printNonTerminal("subroutineDec");

        ifLabelNum = 0;
        whileLabelNum = 0;

         symTable.startSubroutine();

        expectPeek(TokenType.CONSTRUCTOR, TokenType.FUNCTION, TokenType.METHOD);
        var subroutineType = currentToken.type;

        if (subroutineType == TokenType.METHOD) {
            symTable.define("this", className, Kind.ARG);
        };

        printNonTerminal("/subroutineDec");
    }

    // ((type varName) ( ',' type varName)*)?
    void parseParameterList() {
        printNonTerminal("parameterList");

        SymbolTable.Kind kind = Kind.ARG;

        if (!peekTokenIs(TokenType.RPAREN)) // verifica se tem pelo menos uma expressao
        {
            expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
            String type = currentToken.lexeme;

            expectPeek(TokenType.IDENT);
            String name = currentToken.lexeme;
            symTable.define(name, type, kind);

            while (peekTokenIs(TokenType.COMMA)) {
                expectPeek(TokenType.COMMA);
                expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
                type = currentToken.lexeme;

                expectPeek(TokenType.IDENT);
                name = currentToken.lexeme;

                symTable.define(name, type, kind);
            }

        }

        printNonTerminal("/parameterList");
    }

    // letStatement -> 'let' identifier( '[' expression ']' )? '=' expression ';'
    void parseLet() {

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

    // 'while' '(' expression ')' '{' statements '}'
        void parseWhile() {
        printNonTerminal("whileStatement");

        var labelTrue = "WHILE_EXP" + whileLabelNum;
        var labelFalse = "WHILE_END" + whileLabelNum;
        whileLabelNum++;

        vmWriter.writeLabel(labelTrue);

        expectPeek(TokenType.WHILE);
        expectPeek(TokenType.LPAREN);
        parseExpression();

        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(labelFalse);

        expectPeek(TokenType.RPAREN);
        expectPeek(TokenType.LBRACE);
        parseStatements();

        vmWriter.writeGoto(labelTrue); // Go back to labelTrue and check condition
        vmWriter.writeLabel(labelFalse); // Breaks out of while loop because ~(condition) is true

        expectPeek(TokenType.RBRACE);
        printNonTerminal("/whileStatement");
    }


    // 'if' '(' expression ')' '{' statements '}' ( 'else' '{' statements '}' )?
    void parseIf() {
        printNonTerminal("ifStatement");

        var labelTrue = "IF_TRUE" + ifLabelNum;
        var labelFalse = "IF_FALSE" + ifLabelNum;
        var labelEnd = "IF_END" + ifLabelNum;

        ifLabelNum++;
    
        expectPeek(TokenType.IF);
        expectPeek(TokenType.LPAREN);
        parseExpression();
        expectPeek(TokenType.RPAREN);

        vmWriter.writeIf(labelTrue);
        vmWriter.writeGoto(labelFalse);
        vmWriter.writeLabel(labelTrue);
    
        expectPeek(TokenType.LBRACE);
        parseStatements();
        expectPeek(TokenType.RBRACE);
        if (peekTokenIs(TokenType.ELSE)){
            vmWriter.writeGoto(labelEnd);
        }

        vmWriter.writeLabel(labelFalse);

        if (peekTokenIs(TokenType.ELSE))
        {
            expectPeek(TokenType.ELSE);
            expectPeek(TokenType.LBRACE);
            parseStatements();
            expectPeek(TokenType.RBRACE);
            vmWriter.writeLabel(labelEnd);
        }

        printNonTerminal("/ifStatement");
    }

    // statement*
    void parseStatements() {
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

    // letStatement | ifStatement | whileStatement | doStatement | returnStatement
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
                throw new Error("Expected a statement");
        }
    }

    // ReturnStatement -> 'return' expression? ';'
   void parseReturn() {
        printNonTerminal("returnStatement");
        expectPeek(TokenType.RETURN);
        if (!peekTokenIs(TokenType.SEMICOLON)) {
            parseExpression();
        } else {
            vmWriter.writePush(Segment.CONST, 0);
        }
        expectPeek(TokenType.SEMICOLON);
        vmWriter.writeReturn();

        printNonTerminal("/returnStatement");
    }

    // (expression ( ',' expression)* )?
    int parseExpressionList() {
        printNonTerminal("expressionList");
        var numArg = 0;

        if (!peekTokenIs(TokenType.RPAREN)) // verifica se tem pelo menos uma expressao
        {
            parseExpression();
            numArg = 1;
        }

        // procurando as outras
        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            parseExpression();
            numArg++;
        }

        printNonTerminal("/expressionList");
        return numArg;
    }

    private boolean isOperator(TokenType type) {
        return type.ordinal() >= TokenType.PLUS.ordinal() && type.ordinal() <= TokenType.EQ.ordinal();
    }

    // ---------------Gerador de código para expressões ---------------
    // expression -> term (op term)*
    void parseExpression() {
        printNonTerminal("expression");
        parseTerm();
        while (isOperator(peekToken.type)) {
            var ope = peekToken.type;
            expectPeek(peekToken.type);
            parseTerm();
            compileOperators(ope);
        }
        printNonTerminal("/expression");
    }

    // term -> number | identifier | stringConstant | keywordConstant
    void parseTerm() {

        printNonTerminal("term");
        switch (peekToken.type) {
            case NUMBER:
                expectPeek(TokenType.NUMBER);
                System.out.println(currentToken);
                vmWriter.writePush(Segment.CONST, Integer.parseInt(currentToken.lexeme));
                break;

            case STRING: // Rotina do sistema operacional: ->calcular tam da string->empilhar->alocar uma
                         // nova string
                expectPeek(TokenType.STRING);
                var strValue = currentToken.lexeme;
                vmWriter.writePush(Segment.CONST, strValue.length());
                vmWriter.writeCall("String.new", 1);
                for (int i = 0; i < strValue.length(); i++) {
                    vmWriter.writePush(Segment.CONST, strValue.charAt(i));
                    vmWriter.writeCall("String.appendChar", 2);
                }
                break;

            case IDENT:
                expectPeek(TokenType.IDENT);
                Symbol s = symbolTable.resolve(currentToken.lexeme);

                if (peekTokenIs(TokenType.LPAREN) || peekTokenIs(TokenType.DOT)) {
                    parseSubroutineCall();
                } else {
                    if (peekTokenIs(TokenType.LBRACKET)) {
                        expectPeek(TokenType.LBRACKET);
                        parseExpression();
                        vmWriter.writePush(kindSegment2(s.kind()), s.index());
                        vmWriter.writeArithmetic(Command.ADD);
                        expectPeek(TokenType.RBRACKET);
                        vmWriter.writePop(Segment.POINTER, 1); // pop
                        vmWriter.writePush(Segment.THAT, 0); // push
                    } else {
                        vmWriter.writePush(kindSegment2(s.kind()), s.index());
                    }
                }
                break;
            case FALSE:
            case NULL:
            case TRUE:
                expectPeek(TokenType.FALSE, TokenType.NULL, TokenType.TRUE);
                vmWriter.writePush(Segment.CONST, 0);
                if (currentToken.type == TokenType.TRUE)
                    vmWriter.writeArithmetic(Command.NOT);
                break;
            case THIS:
                expectPeek(TokenType.THIS);
                vmWriter.writePush(Segment.POINTER, 0);
                break;

            case LPAREN:
                expectPeek(TokenType.LPAREN);
                parseExpression();
                expectPeek(TokenType.RPAREN);
                break;
            case MINUS:
            case NOT:
                expectPeek(TokenType.MINUS, TokenType.NOT);
                var op = currentToken.type;
                parseTerm();
                if (op == TokenType.MINUS) {
                    vmWriter.writeArithmetic(Command.NEG);
                } else {
                    vmWriter.writeArithmetic(Command.NOT);
                }

                break;
            default:
                throw new Error("term expected");
        }
        printNonTerminal("/term");
    }

    // ---------------Gerador de código para expressões ---------------

    // 'do' subroutineCall ';'
    void parseDo() {
        printNonTerminal("doStatement");
        expectPeek(TokenType.DO);
        expectPeek(TokenType.IDENT);
        parseSubroutineCall();
        expectPeek(TokenType.SEMICOLON);

        printNonTerminal("/doStatement");
    }

    // '{' varDec* statements '}'
    void parseSubroutineBody(String functName, TokenType subroutineType) {

        printNonTerminal("subroutineBody");
        expectPeek(TokenType.LBRACE);
        while (peekTokenIs(TokenType.VAR)) {
            parseVarDec();
        }
        parseStatements();
        expectPeek(TokenType.RBRACE);
        printNonTerminal("/subroutineBody");
    }

    private void expectPeek(TokenType type) {
        if (peekToken.type == type) {
            nextToken();
            xmlOutput.append(String.format("%s\r\n", currentToken.toString()));
        }
    }

    // Funções Auxiliares
    public String XMLOutput() {
        return xmlOutput.toString();
    }

    public String VMOutput() {
        return vmWriter.vmOutput();
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

        throw new Error("Syntax error");
        // throw new Error( "Expected a statement"");

    }

    private Segment kindSegment2(Kind kind) {
        if (kind == Kind.STATIC)
            return Segment.STATIC;
        if (kind == Kind.FIELD)
            return Segment.THIS;
        if (kind == Kind.VAR)
            return Segment.LOCAL;
        if (kind == Kind.ARG)
            return Segment.ARG;
        return null;
    }

    public void compileOperators(TokenType type) {

        if (type == TokenType.ASTERISK) {
            vmWriter.writeCall("Math.multiply", 2);
        } else if (type == TokenType.SLASH) {
            vmWriter.writeCall("Math.divide", 2);
        } else {
            vmWriter.writeArithmetic(typeOperator(type));
        }
    }

    private Command typeOperator(TokenType type) {
        if (type == TokenType.PLUS)
            return Command.ADD;
        if (type == TokenType.MINUS)
            return Command.SUB;
        if (type == TokenType.LT)
            return Command.LT;
        if (type == TokenType.GT)
            return Command.GT;
        if (type == TokenType.EQ)
            return Command.EQ;
        if (type == TokenType.AND)
            return Command.AND;
        if (type == TokenType.OR)
            return Command.OR;
        return null;
    }

}