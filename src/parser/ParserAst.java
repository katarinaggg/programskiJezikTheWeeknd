package parser;

import lexer.token.Token;
import lexer.token.TokenType;
import parser.ast.Ast;
import parser.ast.Expr;
import parser.ast.Stmt;

import java.util.ArrayList;
import java.util.List;

public class ParserAst {

    private final List<Token> tokens;
    private int current = 0;

    public ParserAst(List<Token> tokens) { this.tokens = tokens; }


    private Token peek() { return tokens.get(current); }

    private Token previous() { return tokens.get(current - 1); }

    private boolean isAtEnd() { return peek().type == TokenType.EOF; }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return type == TokenType.EOF;
        return peek().type == type;
    }

    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }


    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        error(peek(), message);
        return null;
    }

    private Token consumeOneOf(String msg, TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) return advance();
        }
        error(peek(), msg);
        return null;
    }

    private void error(Token token, String message) {
        System.err.println("PARSER ERROR:");
        System.err.println("  " + message);
        System.err.println("  Found: " + formatTok(token));
        if (current > 0) {
            Token last = previous();
            System.err.println("  Last read: " + formatTok(last));
        }
        System.exit(1);
    }

    private String formatTok(Token t) {
        return t.type + " '" + t.lexeme + "' at " + t.line + ":" + t.colStart;
    }


    public Ast.Program parse() {
        return parseProgram();
    }


    // program = { func_decl } "XOTWOD" block_main { func_decl } EOF ;
    private Ast.Program parseProgram() {

        // sve funkcije pre main
        List<Ast.FuncDecl> pre = new ArrayList<>();
        while (match(TokenType.CALLOUT)) {
            pre.add(parseFuncDeclAfterCALLOUT());
        }

        // moze imati vise mainova ali cuvamo samo prvi
        // za ostale ce se javiti semanticka greska
        Ast.BlockMain main = null;
        int mainCount = 0;

        while (match(TokenType.XOTWOD)) {
            mainCount++;
            if (mainCount == 1) {
                main = parseBlockMain();
            } else {
                parseBlockMain(); // samo ga parsira i pojede, ne ubacuje se u stablo
            }
        }

        // funkcije posle main
        List<Ast.FuncDecl> post = new ArrayList<>();
        while (match(TokenType.CALLOUT)) {
            post.add(parseFuncDeclAfterCALLOUT());
        }

        consume(TokenType.EOF, "Expected EOF at end of program.");

        Ast.Program program = new Ast.Program(pre, main, post);
        // cuvamo koliko je imao main-ova
        // da bi se bacila greska u semantickoj analizi
        program.mainCount = mainCount;
        return program;
    }


    // block_main = "{" { stmt } "EXODUS" ";" "}" ;
    private Ast.BlockMain parseBlockMain() {
        consume(TokenType.LBRACE, "Expected '{' after XOTWOD.");

        List<Stmt> stmts = new ArrayList<>();

        while (!check(TokenType.EXODUS)) {
            stmts.add(parseStmt());
        }

        Token ex = consume(TokenType.EXODUS, "Expected EXODUS at end of main block.");
        Stmt.Return ret = new Stmt.Return(ex, null);

        // main ne sme nista da vraca
        if (!check(TokenType.SEMICOLON)) {
            error(peek(), "Main block must not return a value. Use 'EXODUS;' without an expression.");
            return null;
        }

        consume(TokenType.SEMICOLON, "Expected ';' after EXODUS");

        consume(TokenType.RBRACE, "Expected '}' after EXODUS;");

        stmts.add(ret);
        return new Ast.BlockMain(stmts);
    }

    // block = "{" { stmt } "}"
    private Ast.Block parseBlock() {
        consume(TokenType.LBRACE, "Expected '{'.");

        List<Stmt> ss = new ArrayList<>();

        while (!check(TokenType.RBRACE)) {
            ss.add(parseStmt());
        }

        consume(TokenType.RBRACE, "Expected '}' after block.");
        return new Ast.Block(ss);
    }

    // func_decl = "CALLOUT" ret_type IDENT "(" param_list_opt ")" block
    private Ast.FuncDecl parseFuncDeclAfterCALLOUT() {

        Ast.RetType rt = parseRetType();
        Token name = consume(TokenType.IDENT, "Expected function name.");

        consume(TokenType.LPAREN, "Expected '(' after function name.");

        List<Ast.Param> params = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            params = parseParamList();
        }

        consume(TokenType.RPAREN, "Expected ')' after parameters.");

        Ast.Block body = parseBlock();

        return new Ast.FuncDecl(rt, name, params, body);
    }

    // ret_type = var_type | "xoVoid"
    private Ast.RetType parseRetType() {
        if (match(TokenType.XOVOID)) {
            return Ast.RetType.ofVoid();
        }
        Ast.VarType vt = parseVarType();
        return Ast.RetType.ofVar(vt);
    }


    // var_type = base_type | "xoArray" var_type
    private Ast.VarType parseVarType() {

        // xoArray var_type -> još jedna dimenzija
        // zovemo rekurzivno
        if (match(TokenType.XOARRAY)) {
            Ast.VarType inner = parseVarType();
            return new Ast.VarType(inner);
        }
        // pokupili smo sve xoArray (dimenzije) i dosli do base tipa elementa

        // sada očekujemo jedan od base_type tokena
        Token base = consumeOneOf(
                "Expected base type (xoNum/xoDouble/xoChar/xoBool/xoString).",
                TokenType.XONUM,
                TokenType.XODOUBLE,
                TokenType.XOCHAR,
                TokenType.XOBOOL,
                TokenType.XOSTRING
        );

        Ast.BaseType bt = switch (base.type) {
            case XONUM -> Ast.BaseType.XONUM;
            case XODOUBLE -> Ast.BaseType.XODOUBLE;
            case XOCHAR -> Ast.BaseType.XOCHAR;
            case XOBOOL -> Ast.BaseType.XOBOOL;
            case XOSTRING -> Ast.BaseType.XOSTRING;
            default -> { error(base, "Unknown base type."); yield null; }
        };

        return new Ast.VarType(bt);
    }


    // param_list_opt = ε | param { "," param }
    private List<Ast.Param> parseParamList() {
        List<Ast.Param> ps = new ArrayList<>();
        ps.add(parseParam());
        while (match(TokenType.COMMA)) {
            ps.add(parseParam());
        }
        return ps;
    }

    // param = var_type IDENT
    private Ast.Param parseParam() {
        Ast.VarType t = parseVarType();
        Token name = consume(TokenType.IDENT, "Expected parameter name.");
        return new Ast.Param(t, name);
    }


    // stmt = var_decl
    //      | assign_stmt ";"
    //      | io_stmt ";"
    //      | if_stmt
    //      | loop_stmt
    //      | return_stmt ";"
    //      | block ;

    private Stmt parseStmt() {

        if (check(TokenType.CALLOUT)) return parseVarDecl();

        if (check(TokenType.EXODUS)) {
            Stmt.Return r = parseReturnStmt();
            //consume(TokenType.SEMICOLON, "Expected ';' after return.");
            return r;
        }

        if (check(TokenType.LISTEN) || check(TokenType.ECHO)) {
            Stmt rv = parseIO();
            consume(TokenType.SEMICOLON, "Expected ';' after I/O statement.");
            return rv;
        }

        if (check(TokenType.TRYME)) return parseIf();

        if (check(TokenType.REPEATAFTERME) || check(TokenType.TIMELESS))
            return parseLoop();

        if (check(TokenType.LBRACE)) return parseBlock();

        // unary_step kao samostalna naredba
        if (check(TokenType.RISE) || check(TokenType.FADE)) {
            Token op = advance();          // RISE | FADE
            Ast.LValue lv = parseLValue(); // mora IDENT ili IDENT[index]
            consume(TokenType.SEMICOLON, "Expected ';' after rise/fade.");

            Expr.UnaryStep stepExpr = new Expr.UnaryStep(op, lv);
            return new Stmt.UnaryStepStmt(stepExpr);
        }

        // call kao samostalna naredba: IDENT "(" ... ")" ";"
        if (check(TokenType.IDENT) && checkNext(TokenType.LPAREN)) {
            Expr.Call call = parseCallExpr();
            consume(TokenType.SEMICOLON, "Expected ';' after function call.");
            return new Stmt.CallStmt(call);
        }

        // assign
        Stmt.Assign a = parseAssign();
        consume(TokenType.SEMICOLON, "Expected ';' after assignment.");
        return a;
    }


    // var_decl = "CALLOUT" var_type IDENT var_init_opt ";"
    private Stmt.VarDecl parseVarDecl() {
        Token kw = consume(TokenType.CALLOUT, "Expected CALLOUT keyword.");
        Ast.VarType vt = parseVarType();
        Token name = consume(TokenType.IDENT, "Expected variable name.");

        Expr init = null;
        if (match(TokenType.ASSIGN)) { // "~>"
            init = parseExpr();
        }

        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.");

        return new Stmt.VarDecl(kw, vt, name, init);
    }


    // assign_stmt = lvalue assign_op expr
    private Stmt.Assign parseAssign() {
        Ast.LValue target = parseLValue();
        Token op = consume(TokenType.ASSIGN, "Expected '~>' in assignment.");
        Expr value = parseExpr();
        return new Stmt.Assign(target, op, value);
    }

    // lvalue = IDENT { "[" expr "]" }
    private Ast.LValue parseLValue() {
        Token name = consume(TokenType.IDENT, "Expected identifier in lvalue.");
        List<Expr> idx = new ArrayList<>();

        while (match(TokenType.LBRACKET)) {
            idx.add(parseExpr());
            consume(TokenType.RBRACKET, "Expected ']' after index.");
        }

        return new Ast.LValue(name, idx);
    }


    private Stmt parseIO() {

        if (match(TokenType.LISTEN)) {
            Token kw = previous();
            consume(TokenType.LPAREN, "Expected '(' after LISTEN.");
            Ast.LValue lv = parseLValue();
            consume(TokenType.RPAREN, "Expected ')' after LISTEN.");
            return new Stmt.Listen(kw, lv);
        }

        if (match(TokenType.ECHO)) {
            Token kw = previous();
            consume(TokenType.LPAREN, "Expected '(' after ECHO.");
            Expr e = parseExpr();
            consume(TokenType.RPAREN, "Expected ')' after ECHO.");
            return new Stmt.Echo(kw, e);
        }

        error(peek(), "Invalid I/O statement.");
        return null;
    }



    private Stmt.Return parseReturnStmt() {
        Token ex = consume(TokenType.EXODUS, "Expected EXODUS in return.");
        Expr val = null;
        if (!check(TokenType.SEMICOLON)) {
            val = parseExpr();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after EXODUS.");
        return new Stmt.Return(ex, val);
    }


    private Stmt.If parseIf() {

        consume(TokenType.TRYME, "Expected tryMe.");

        consume(TokenType.LPAREN, "Expected '(' after tryMe.");
        Expr cond = parseExpr();
        consume(TokenType.RPAREN, "Expected ')' after tryMe condition.");

        Ast.Block thenBlock = parseBlock();

        List<Stmt.If.ElseIf> elsifs = new ArrayList<>();

        while (match(TokenType.NEXT)) {
            consume(TokenType.LPAREN, "Expected '(' after next.");
            Expr c = parseExpr();
            consume(TokenType.RPAREN, "Expected ')' after next condition.");
            Ast.Block b = parseBlock();
            elsifs.add(new Stmt.If.ElseIf(c, b));
        }

        Stmt.If.Else elseBranch = null;
        if (match(TokenType.ORNAH)) {
            Ast.Block b = parseBlock();
            elseBranch = new Stmt.If.Else(b);
        }

        return new Stmt.If(cond, thenBlock, elsifs, elseBranch);
    }



    private Stmt parseLoop() {

        if (match(TokenType.REPEATAFTERME))
            return parseRepeat();

        if (match(TokenType.TIMELESS))
            return parseTimeless();

        error(peek(), "Invalid loop statement.");
        return null;
    }

    // timeless "(" expr ")" block
    private Stmt.Timeless parseTimeless() {
        consume(TokenType.LPAREN, "Expected '(' after timeless.");
        Expr cond = parseExpr();
        consume(TokenType.RPAREN, "Expected ')' after timeless condition.");
        Ast.Block b = parseBlock();
        return new Stmt.Timeless(cond, b);
    }

    // repeatAfterMe "(" for_init ";" for_cond_opt ";" for_step_opt ")" block
    private Stmt.RepeatAfterMe parseRepeat() {
        consume(TokenType.LPAREN, "Expected '(' after repeatAfterMe.");

        //  init: decl_for_init | assign_stmt | ε
        Stmt.RepeatAfterMe.ForInit init = null;

        if (match(TokenType.CALLOUT)) {
            // sacuvaj bas ovaj CALLOUT token (NE previous() kasnije!)
            Token kw = previous();
            Ast.VarType vt = parseVarType();
            Token name = consume(TokenType.IDENT, "Expected variable name in for-init decl.");

            Expr initExpr = null;
            if (match(TokenType.ASSIGN)) {
                initExpr = parseExpr();
            }

            init = new Stmt.RepeatAfterMe.InitDecl(kw, vt, name, initExpr);
        } else if (check(TokenType.IDENT)) {
            // assign_stmt bez ';' (jer smo u headeru)
            Ast.LValue lv = parseLValue();
            Token op = consume(TokenType.ASSIGN, "Expected '~>' in init assign.");
            Expr val = parseExpr();
            init = new Stmt.RepeatAfterMe.InitAssign(lv, op, val);
        }
        consume(TokenType.SEMICOLON, "Expected ';' after loop init.");

        //  cond: expr | ε
        Expr cond = null;
        if (!check(TokenType.SEMICOLON)) {
            cond = parseExpr();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after loop condition.");

        //  step: assign_stmt | unary_step | ε
        Stmt.RepeatAfterMe.ForStep step = null;

        if (!check(TokenType.RPAREN)) {
            if (check(TokenType.IDENT)) {
                Ast.LValue lv = parseLValue();
                Token op = consume(TokenType.ASSIGN, "Expected '~>' in loop step.");
                Expr val = parseExpr();
                step = new Stmt.RepeatAfterMe.StepAssign(lv, op, val);
            } else if (check(TokenType.RISE) || check(TokenType.FADE)) {
                Token op = advance();          // RISE | FADE
                Ast.LValue lv = parseLValue(); // lvalue
                step = new Stmt.RepeatAfterMe.StepUnary(new Expr.UnaryStep(op, lv));
            } else {
                error(peek(), "Expected assignment or rise/fade step in for header.");
                return null;
            }
        }

        consume(TokenType.RPAREN, "Expected ')' after loop header.");

        //  body
        Ast.Block body = parseBlock();

        return new Stmt.RepeatAfterMe(init, cond, step, body);
    }


    // expr = or_expr
    private Expr parseExpr() {
        return parseOr();
    }

    // or_expr = and_expr { "xoOr" and_expr }
    private Expr parseOr() {
        Expr expr = parseAnd();

        while (match(TokenType.XOOR)) {
            Token op = previous();
            Expr right = parseAnd();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    // and_expr = not_expr { "xoAnd" not_expr }
    private Expr parseAnd() {
        Expr expr = parseNot();

        while (match(TokenType.XOAND)) {
            Token op = previous();
            Expr right = parseNot();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    // not_expr = "xoNot" not_expr | rel_expr
    private Expr parseNot() {
        if (match(TokenType.XONOT)) {
            Token op = previous();
            Expr right = parseNot();
            return new Expr.Unary(op, right);
        }
        return parseRel();
    }

    // rel_expr = add_expr { rel_op add_expr }
    private Expr parseRel() {
        Expr expr = parseAdd();

        while (match(TokenType.EQ, TokenType.NEQ,
                TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE)) {
            Token op = previous();
            Expr right = parseAdd();
            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    // add_expr = mul_expr { ("+" | "-") mul_expr }
    private Expr parseAdd() {
        Expr expr = parseMul();

        while (match(TokenType.ADD, TokenType.SUBTRACT)) {
            Token op = previous();
            Expr right = parseMul();
            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    // mul_expr = unary_expr { ("*" | "/" | "%") unary_expr }
    private Expr parseMul() {
        Expr expr = parseUnaryExpr();

        while (match(TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.PERCENT)) {
            Token op = previous();
            Expr right = parseUnaryExpr();
            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }


    // pomocne funkcije za kastovanje
    private Ast.BaseType tokenToBaseType(Token t) {
        return switch (t.type) {
            case XONUM -> Ast.BaseType.XONUM;
            case XODOUBLE -> Ast.BaseType.XODOUBLE;
            case XOCHAR -> Ast.BaseType.XOCHAR;
            case XOBOOL -> Ast.BaseType.XOBOOL;
            case XOSTRING -> Ast.BaseType.XOSTRING;
            default -> {
                error(t, "Invalid base type in cast.");
                yield null;
            }
        };
    }

    private boolean checkNextBaseType() {
        if (current + 1 >= tokens.size()) return false;
        TokenType t = tokens.get(current + 1).type;
        return t == TokenType.XONUM
                || t == TokenType.XODOUBLE
                || t == TokenType.XOCHAR
                || t == TokenType.XOBOOL
                || t == TokenType.XOSTRING;
    }


    // unary_expr = "(" base_type ")" unary_expr
    //            | (+ | -) unary_expr
    //            | (rise | fade) lvalue
    //            | primary
    private Expr parseUnaryExpr() {

        if (check(TokenType.LPAREN) && checkNextBaseType()) {
            consume(TokenType.LPAREN, "Expected '(' in cast.");
            Token typeTok = advance();
            Ast.BaseType targetType = tokenToBaseType(typeTok);
            consume(TokenType.RPAREN, "Expected ')' after cast type.");
            Expr inner = parseUnaryExpr();
            return new Expr.Cast(targetType, inner);
        }

        if (match(TokenType.ADD, TokenType.SUBTRACT)) {
            Token op = previous();
            Expr right = parseUnaryExpr();
            return new Expr.Unary(op, right);
        }

        if (match(TokenType.RISE, TokenType.FADE)) {
            Token op = previous();
            Ast.LValue lv = parseLValue();
            return new Expr.UnaryStep(op, lv);
        }

        return parsePrimary();
    }


    // primary: literals, lvalue, array_lit, (expr)
    private Expr parsePrimary() {

        if (match(TokenType.INT_LIT))
            return new Expr.NumLit(previous(), (int) previous().literal);

        if (match(TokenType.DOUBLE_LIT))
            return new Expr.RealLit(previous(), (double) previous().literal);

        if (match(TokenType.STRING_LIT))
            return new Expr.StringLit(previous(), (String) previous().literal);

        if (match(TokenType.CHAR_LIT))
            return new Expr.CharLit(previous(), (char) previous().literal);

        if (match(TokenType.BOOL_LIT))
            return new Expr.BoolLit(previous(), (boolean) previous().literal);

        if (check(TokenType.LBRACKET))
            return parseArrayLiteral();

        // razlikujemo slucaj kada je ime promenljive i kada je poziv funkcije
        if (check(TokenType.IDENT)) {
            if (checkNext(TokenType.LPAREN)) { // ako posle imena ide zagrada -> poziv fje
                return parseCallExpr();
            } else {
                Ast.LValue lv = parseLValue();
                return new Expr.LValueRef(lv);
            }
        }

        if (match(TokenType.LPAREN)) {
            Expr inner = parseExpr();
            consume(TokenType.RPAREN, "Expected ')' after grouped expression.");
            return new Expr.Grouping(inner);
        }

        error(peek(), "Unexpected token in expression.");
        return null;
    }

    // array_lit = "[" [ elem { ',' elem } ] "]"
    private Expr.ArrayLit parseArrayLiteral() {
        consume(TokenType.LBRACKET, "Expected '[' to start array literal.");

        List<Expr> elems = new ArrayList<>();

        if (!check(TokenType.RBRACKET)) {
            elems.add(parseArrayElement());
            while (match(TokenType.COMMA)) {
                elems.add(parseArrayElement());
            }
        }
        consume(TokenType.RBRACKET, "Expected ']' after array literal.");
        return new Expr.ArrayLit(elems);
    }

    private Expr parseArrayElement() {
        if (check(TokenType.LBRACKET))
            return parseArrayLiteral();
        return parseExpr();
    }


    // parsiranje poziva funkcije
    // call_expr = IDENT "(" [ expr { "," expr } ] ")"
    private Expr.Call parseCallExpr() {
        Token name = consume(TokenType.IDENT, "Expected function name.");
        consume(TokenType.LPAREN, "Expected '(' after function name.");
        List<Expr> args = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            args.add(parseExpr());
            while (match(TokenType.COMMA)) args.add(parseExpr());
        }
        consume(TokenType.RPAREN, "Expected ')' after arguments.");
        return new Expr.Call(name, args);
    }

}
