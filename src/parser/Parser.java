package parser;

import lexer.token.Token;
import lexer.token.TokenType;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        error(peek(), message);
        return null;
    }

    private void error(Token token, String message) {
        System.err.println("PARSER ERROR at line " + token.line + ", col " + token.colStart + ": " + message);
        System.err.println("  Found: " + token.lexeme);
        System.exit(1);
    }

    public Node parseProgram() {
        Node program = new Node("Program");

        // Parsiraj sve funkcije koje dolaze pre glavnog programa
        while (check(TokenType.CALLOUT)) {

            // CALLOUT <type> <ident> '(' → funkcija
            if (current + 3 < tokens.size()
                    && (tokens.get(current + 1).type == TokenType.XONUM
                    || tokens.get(current + 1).type == TokenType.XODOUBLE
                    || tokens.get(current + 1).type == TokenType.XOCHAR
                    || tokens.get(current + 1).type == TokenType.XOBOOL
                    || tokens.get(current + 1).type == TokenType.XOSTRING
                    || tokens.get(current + 1).type == TokenType.XOVOID)
                    && tokens.get(current + 2).type == TokenType.IDENT
                    && tokens.get(current + 3).type == TokenType.LPAREN) {

                program.addChild(parseFunctionDecl());
            } else {
                program.addChild(parseDeclaration());
            }
        }

        //  Glavni program (XOTWOD)
        consume(TokenType.XOTWOD, "Program must start with 'XOTWOD'.");
        Node mainBlock = parseBlock();

        // Provera da EXODUS postoji na kraju
        if (mainBlock.getChildren().isEmpty()) {
            throw new RuntimeException("PARSER ERROR: Main block cannot be empty.");
        }

        Node lastChild = mainBlock.getChildren().get(mainBlock.getChildren().size() - 1);
        if (!"Exodus".equals(lastChild.getType())) {
            throw new RuntimeException("PARSER ERROR: Main block must end with 'EXODUS;'.");
        }

        program.addChild(mainBlock);
        return program;
    }


    private Node parseFunctionDecl() {
        Node func = new Node("FunctionDecl");

        // mora da počne sa CALLOUT
        consume(TokenType.CALLOUT, "Expected 'CALLOUT' at start of function declaration.");

        // tip povratne vrednosti (xoNum, xoDouble, xoBool, xoVoid, ...)
        Node returnType = consumeType();
        func.addChild(returnType);

        // ime funkcije
        Token name = consume(TokenType.IDENT, "Expected function name after type.");
        func.addChild(new Node("FuncName", name.lexeme));

        // parametri u zagradama
        consume(TokenType.LPAREN, "Expected '(' after function name.");
        Node params = new Node("Parameters");

        if (!check(TokenType.RPAREN)) {  // ako funkcija ima parametre
            do {
                Node paramType = consumeType();
                Token paramName = consume(TokenType.IDENT, "Expected parameter name.");
                Node param = new Node("Param");
                param.addChild(paramType);
                param.addChild(new Node("Name", paramName.lexeme));
                params.addChild(param);
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RPAREN, "Expected ')' after parameter list.");
        func.addChild(params);

        // telo funkcije
        Node body = parseBlock();
        func.addChild(body);

        // Ako je tip xoVoid → dozvoli da nema EXODUS
        if (returnType.getValue().equals("xoVoid")) {
            return func;
        }

        // Novi pristup: proveravamo da li se bilo gde unutar tela nalazi EXODUS
        boolean hasExodus = containsExodus(body);

        if (!hasExodus) {
            System.err.println("Warning: Function '" + name.lexeme +
                    "' does not contain any 'EXODUS' statement for non-void return type.");
        }

        return func;
    }

    /**
     * Rekurzivno proverava da li se bilo gde u telu funkcije nalazi čvor tipa 'Exodus'.
     */
    private boolean containsExodus(Node node) {
        if (node.getType().equals("Exodus")) {
            return true;
        }
        for (Node child : node.getChildren()) {
            if (containsExodus(child)) {
                return true;
            }
        }
        return false;
    }


//    private Node parseFunctionDecl() {
//        Node func = new Node("FunctionDecl");
//
//        // mora da počne sa CALLOUT
//        consume(TokenType.CALLOUT, "Expected 'CALLOUT' at start of function declaration.");
//
//        // tip povratne vrednosti (xoNum, xoDouble, xoBool, xoVoid, ...)
//        Node returnType = consumeType();
//        func.addChild(returnType);
//
//        // ime funkcije
//        Token name = consume(TokenType.IDENT, "Expected function name after type.");
//        func.addChild(new Node("FuncName", name.lexeme));
//
//        // parametri u zagradama
//        consume(TokenType.LPAREN, "Expected '(' after function name.");
//        Node params = new Node("Parameters");
//
//        if (!check(TokenType.RPAREN)) {  // ako funkcija ima parametre
//            do {
//                Node paramType = consumeType();
//                Token paramName = consume(TokenType.IDENT, "Expected parameter name.");
//                Node param = new Node("Param");
//                param.addChild(paramType);
//                param.addChild(new Node("Name", paramName.lexeme));
//                params.addChild(param);
//            } while (match(TokenType.COMMA));
//        }
//
//        consume(TokenType.RPAREN, "Expected ')' after parameter list.");
//        func.addChild(params);
//
//        // telo funkcije
//        Node body = parseBlock();
//        func.addChild(body);
//
//        // validacija: ako je xoVoid → ne mora imati EXODUS vrednost
//        if (returnType.getValue().equals("xoVoid")) {
//            return func; // xoVoid ne mora ništa da vraća
//        }
//
//        // ostali tipovi moraju imati EXODUS
//        Node last = body.getChildren().get(body.getChildren().size() - 1);
//        if (!"Exodus".equals(last.getType())) {
//            throw new RuntimeException("PARSER ERROR: Function must end with 'EXODUS;'.");
//        }
//
//        return func;
//    }

    private Node parseBlock() {
        Node block = new Node("Block");
        consume(TokenType.LBRACE, "Expected '{' to start block.");
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            block.addChild(parseStatement());
        }
        consume(TokenType.RBRACE, "Expected '}' to close block.");
        return block;
    }


    private Node parseStatement() {
        Token token = peek();

        //Prepoznavanje deklaracija i funkcija
        if (token.type == TokenType.XONUM || token.type == TokenType.XODOUBLE ||
                token.type == TokenType.XOCHAR || token.type == TokenType.XOBOOL ||
                token.type == TokenType.XOSTRING || token.type == TokenType.XOVOID ||
                token.type == TokenType.XOARRAY) {

            // ako je <type> <ident> '(' → funkcija
            if ((current + 2 < tokens.size()) && tokens.get(current + 2).type == TokenType.LPAREN) {
                return parseFunctionDecl();
            }

            // inače → obična deklaracija
            return parseDeclaration();
        }


        switch (token.type) {
            case CALLOUT:
                return parseDeclaration();
            case LISTEN:
                return parseListen();
            case ECHO:
                return parseEcho();
            case TRYME:
                return parseIf();
            case REPEATAFTERME:
            case TIMELESS:
                return parseLoop();
            case EXODUS:
                return parseExodus();
            case XONUM:
            case XODOUBLE:
            case XOCHAR:
            case XOBOOL:
            case XOSTRING:
                return parseFunctionDecl(); //
            case IDENT:
                System.out.println("DEBUG → peek: " + peek().lexeme +
                        " next: " + tokens.get(current + 1).lexeme);
                if (checkNext(TokenType.ASSIGN)) {
                    System.out.println("DEBUG → found assignment");
                    return parseAssignment();
                } else {
                    Node exprStmt = new Node("ExprStmt");
                    exprStmt.addChild(parseExpr());
                    consume(TokenType.SEMICOLON, "Expected ';' after expression statement.");
                    return exprStmt;
                }
            default:
                error(token, "Unexpected statement start: " + token.lexeme);
                return null;
        }
    }
    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }


    private Node parseDeclaration() {
        Token callout = consume(TokenType.CALLOUT, "Expected 'CALLOUT' at the beginning of declaration.");
        Node declNode = new Node("Declaration");
        declNode.addChild(new Node("Keyword", callout.lexeme));

        // Čvor za tip
        Node typeNode = new Node("Type");

        // 1) Ako počinje sa xoArray -> imamo (možda višestruke) nivoe niza
        if (match(TokenType.XOARRAY)) {
            // bar jedan nivo niza
            typeNode.addChild(new Node("ArrayLevel", previous().lexeme));

            // dodatni xoArray nivo(i)
            while (match(TokenType.XOARRAY)) {
                typeNode.addChild(new Node("ArrayLevel", previous().lexeme));
            }

            // sad očekujemo bazni tip: xoNum/xoDouble/xoChar/xoBool/xoString
            if (match(TokenType.XONUM, TokenType.XODOUBLE, TokenType.XOCHAR,
                    TokenType.XOBOOL, TokenType.XOSTRING)) {
                typeNode.addChild(new Node("BaseType", previous().lexeme));
            } else {
                error(peek(), "Expected base type after xoArray (xoNum, xoDouble, xoChar, xoBool, xoString).");
                return null; // error() će već izaći iz programa
            }

            // 2) Inače očekujemo direktan bazni tip bez niza
        } else if (match(TokenType.XONUM, TokenType.XODOUBLE, TokenType.XOCHAR,
                TokenType.XOBOOL, TokenType.XOSTRING)) {
            typeNode.addChild(new Node("BaseType", previous().lexeme));
        } else {
            error(peek(), "Expected data type after CALLOUT.");
            return null;
        }

        declNode.addChild(typeNode);

        // Ime promenljive
        Token ident = consume(TokenType.IDENT, "Expected identifier after type declaration.");
        declNode.addChild(new Node("Ident", ident.lexeme));

        // Opciona dodela: ~> izraz
        if (match(TokenType.ASSIGN)) {
            declNode.addChild(parseExpr());
        }

        consume(TokenType.SEMICOLON, "Expected ';' after declaration.");
        return declNode;
    }

    private Node parseListen() {
        Token listenToken = advance(); // LISTEN

        consume(TokenType.LPAREN, "Expected '(' after LISTEN.");
        Token varName = consume(TokenType.IDENT, "Expected variable name inside LISTEN().");
        consume(TokenType.RPAREN, "Expected ')' after variable name.");
        consume(TokenType.SEMICOLON, "Expected ';' after LISTEN(...) statement.");

        Node node = new Node("IOStmt", listenToken.lexeme);
        node.addChild(new Node("Ident", varName.lexeme));
        return node;
    }

    // pomoćna funkcija za prepoznavanje tipa
    private Node consumeType() {
        if (match(TokenType.XONUM, TokenType.XODOUBLE, TokenType.XOCHAR,
                TokenType.XOBOOL, TokenType.XOSTRING, TokenType.XOARRAY, TokenType.XOVOID)) {
            return new Node("Type", previous().lexeme);
        }
        error(peek(), "Expected data type after CALLOUT.");
        return null;
    }
    private Node parseEcho() {
        Token keyword = advance();  // ECHO
        Node echoNode = new Node("ECHO", keyword.lexeme);

        if (keyword.type == TokenType.ECHO) {
            consume(TokenType.LPAREN, "Expected '(' after ECHO.");
            echoNode.addChild(parseExpr());  // može biti string literal, identifikator ili izraz
            consume(TokenType.RPAREN, "Expected ')' after ECHO expression.");
        }

        consume(TokenType.SEMICOLON, "Expected ';' after ECHO statement.");
        return echoNode;
    }


    private Node parseIf() {
        Node ifStmt = new Node("IfStmt");

        consume(TokenType.TRYME, "Expected TRYME keyword.");
        consume(TokenType.LPAREN, "Expected '(' after TRYME.");
        ifStmt.addChild(parseExpr()); // uslov
        consume(TokenType.RPAREN, "Expected ')' after condition.");
        ifStmt.addChild(parseBlock()); // telo TRYME

        // može imati više NEXT grana (else if)
        while (match(TokenType.NEXT)) {
            Node nextStmt = new Node("ElseIf");
            consume(TokenType.LPAREN, "Expected '(' after NEXT.");
            nextStmt.addChild(parseExpr());
            consume(TokenType.RPAREN, "Expected ')' after condition.");
            nextStmt.addChild(parseBlock());
            ifStmt.addChild(nextStmt);
        }

        // opcionalna ORNAH grana (else)
        if (match(TokenType.ORNAH)) {
            Node elseStmt = new Node("Else");
            elseStmt.addChild(parseBlock());
            ifStmt.addChild(elseStmt);
        }

        return ifStmt;
    }

    private Node parseLoop() {
        Token loopType = advance(); // REPEATAFTERME ili TIMELESS
        Node loop = new Node("Loop", loopType.lexeme);

        consume(TokenType.LPAREN, "Expected '(' after loop keyword.");

        if (loopType.type == TokenType.REPEATAFTERME) {
            // repeatAfterMe(init; condition; increment)
            //  Inicijalizacija
            if (!check(TokenType.SEMICOLON)) {
                if (check(TokenType.CALLOUT)) {
                    loop.addChild(parseDeclaration());
                } else if (check(TokenType.IDENT) && checkNext(TokenType.ASSIGN)) {
                    loop.addChild(parseAssignment());
                } else {
                    loop.addChild(parseExpr());
                    consume(TokenType.SEMICOLON, "Expected ';' after initialization expression.");
                }
            } else {
                advance(); // pojedi ';' ako nema ništa pre
            }

            //  Uslov
            if (!check(TokenType.SEMICOLON)) {
                loop.addChild(parseExpr());
            }
            consume(TokenType.SEMICOLON, "Expected ';' after condition.");

            //  Inkrement/dekrement
            if (!check(TokenType.RPAREN)) {
                loop.addChild(parseExpr());
            }
            consume(TokenType.RPAREN, "Expected ')' after for loop header.");

        } else if (loopType.type == TokenType.TIMELESS) {
            // timeless(condition)
            loop.addChild(parseExpr()); // direktno uslov
            consume(TokenType.RPAREN, "Expected ')' after timeless condition.");
        }

        // telo
        loop.addChild(parseBlock());
        return loop;
    }

    private Node parseAssignment() {
        // provera da li zaista imamo dodelu
        if (!checkNext(TokenType.ASSIGN)) {
            // nije dodela, tretiraj kao običan izraz
            Node exprStmt = new Node("ExprStmt");
            exprStmt.addChild(parseExpr());
            consume(TokenType.SEMICOLON, "Expected ';' after expression statement.");
            return exprStmt;
        }

        // inače - normalna dodela
        Token ident = consume(TokenType.IDENT, "Expected identifier at start of assignment.");
        Node assign = new Node("Assignment");
        assign.addChild(new Node("Ident", ident.lexeme));

        consume(TokenType.ASSIGN, "Expected '~>' after variable name.");
        assign.addChild(parseExpr()); // izraz sa desne strane

        consume(TokenType.SEMICOLON, "Expected ';' after assignment.");
        return assign;
    }

/// ===================== EXPRESSIONS =====================

    // Glavna ulazna tačka za izraze
    private Node parseExpr() {
        return parseOr();
    }

    // OR (najniži prioritet)
    private Node parseOr() {
        Node expr = parseAnd();

        while (match(TokenType.XOOR)) { // npr. 'xoOr'
            Token operator = previous();
            Node right = parseAnd();
            Node newNode = new Node("BinaryOp", operator.lexeme);
            newNode.addChild(expr);
            newNode.addChild(right);
            expr = newNode;
        }
        return expr;
    }

    // AND
    private Node parseAnd() {
        Node expr = parseEquality();

        while (match(TokenType.XOAND)) { // npr. 'xoAnd'
            Token operator = previous();
            Node right = parseEquality();
            Node newNode = new Node("BinaryOp", operator.lexeme);
            newNode.addChild(expr);
            newNode.addChild(right);
            expr = newNode;
        }
        return expr;
    }

    // ==, !=
    private Node parseEquality() {
        Node expr = parseComparison();

        while (match(TokenType.EQ, TokenType.NEQ)) {
            Token operator = previous();
            Node right = parseComparison();
            Node newNode = new Node("BinaryOp", operator.lexeme);
            newNode.addChild(expr);
            newNode.addChild(right);
            expr = newNode;
        }
        return expr;
    }

    // <, >, <=, >=
    private Node parseComparison() {
        Node expr = parseTerm();

        while (match(TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE)) {
            Token operator = previous();
            Node right = parseTerm();
            Node newNode = new Node("BinaryOp", operator.lexeme);
            newNode.addChild(expr);
            newNode.addChild(right);
            expr = newNode;
        }
        return expr;
    }

    // ===================== TERM =====================
    private Node parseTerm() {
        Node node = parseFactor();
        while (match(TokenType.ADD, TokenType.SUBTRACT)) {
            Token op = previous();
            Node opNode = new Node("BinaryOp", op.lexeme);
            opNode.addChild(node);
            opNode.addChild(parseFactor());
            node = opNode;
        }
        return node;
    }
    // Množenje, deljenje, modulo
    private Node parseFactor() {
        Node node = parseUnary();
        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.PERCENT)) {
            Token op = previous();
            Node opNode = new Node("BinaryOp", op.lexeme);
            opNode.addChild(node);
            opNode.addChild(parseUnary());
            node = opNode;
        }
        return node;
    }


    private Node parseUnary() {
        if (match(TokenType.SUBTRACT, TokenType.XONOT, TokenType.RISE, TokenType.FADE)) {
            Token op = previous();
            Node node = new Node("UnaryOp", op.lexeme);
            node.addChild(parseUnary());
            return node;
        }
        return parsePrimary();
    }

    private Node parsePrimary() {
        // literali
        if (match(TokenType.INT_LIT, TokenType.DOUBLE_LIT, TokenType.STRING_LIT,
                TokenType.CHAR_LIT, TokenType.BOOL_LIT)) {
            return new Node("Literal", previous().lexeme);
        }

        if (match(TokenType.IDENT)) {
            Token ident = previous();

            // ako sledi '(' → poziv funkcije
            if (match(TokenType.LPAREN)) {
                Node call = new Node("FunctionCall", ident.lexeme);

                if (!check(TokenType.RPAREN)) {
                    do {
                        call.addChild(parseExpr());
                    } while (match(TokenType.COMMA));
                }

                consume(TokenType.RPAREN, "Expected ')' after function call arguments.");
                return call;
            }

            // inače je obična promenljiva
            Node varNode = new Node("Ident", ident.lexeme);

            while (match(TokenType.LBRACKET)) {
                Node indexNode = new Node("Index");
                indexNode.addChild(varNode);
                indexNode.addChild(parseExpr());
                consume(TokenType.RBRACKET, "Expected ']' after index expression.");
                varNode = indexNode;
            }

            return varNode;
        }

        // Ako je izraz niz (literal niza, tj. '{ ... }')
        if (match(TokenType.LBRACKET)) {
            Node arrayNode = new Node("ArrayLiteral");

            // ako odmah sledi zatvaranje, prazan niz
            if (!check(TokenType.RBRACKET)) {
                do {
                    // rekurzivno parsiraj element – može biti broj, drugi niz, izraz itd.
                    arrayNode.addChild(parseExpr());
                } while (match(TokenType.COMMA));  // elementi odvojeni zarezima
            }

            consume(TokenType.RBRACKET, "Expected '}' to close array literal.");
            return arrayNode;
        }


        // izrazi u zagradama
        if (match(TokenType.LPAREN)) {
            Node expr = parseExpr();
            consume(TokenType.RPAREN, "Expected ')' after expression.");
            return expr;
        }

        error(peek(), "Unexpected token in expression.");
        return null;
    }
    private Node parseExodus() {
        consume(TokenType.EXODUS, "Expected EXODUS keyword inside block.");
        Node exodus = new Node("Exodus");

        // Ako nije odmah ;, postoji izraz koji treba parsirati
        if (!check(TokenType.SEMICOLON)) {
            exodus.addChild(parseExpr());
        }

        // EXODUS uvek mora da ima ;
        consume(TokenType.SEMICOLON, "Expected ';' after EXODUS statement.");

        return exodus;
    }


}
