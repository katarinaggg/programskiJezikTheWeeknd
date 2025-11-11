package parser.ast;

import java.util.List;
import lexer.token.Token;

public final class Ast {

    //  KOREN PROGRAMA

    // { func_decl } "XOTWOD" block_main { func_decl } EOF ;
    public static final class Program extends NodeBase{
        public final List<FuncDecl> preMain;
        public final BlockMain main;
        public final List<FuncDecl> postMain;

        public Program(List<FuncDecl> preMain, BlockMain main, List<FuncDecl> postMain) {
            this.preMain = preMain;
            this.main = main;
            this.postMain = postMain;
            addAll(preMain);
            add(main);
            addAll(postMain);
        }

        // postavljamo ime tipa
        @Override
        protected String label() {
            return "Program";
        }
    }

    // GLAVNI BLOK MAIN FUNKCIJE
    // "{" { stmt } "EXODUS" "}" ;
    //  provera EXODUS ce se dodati u parseru
    public static final class BlockMain extends NodeBase{
        public final List<Stmt> stmts;

        public BlockMain(List<Stmt> stmts) {
            this.stmts = stmts;
            addAll(stmts);
        }

        @Override
        protected String label() {
            return "BlockMain";
        }
    }


    // base_type  = "xoNum" | "xoDouble" | "xoChar" | "xoBool" | "xoString" ;
    public enum BaseType {XONUM, XODOUBLE, XOCHAR, XOBOOL, XOSTRING}


    //var_type  = base_type | "xoArray" base_type ;
    public static final class VarType extends NodeBase{
        public final BaseType type;
        public final boolean isArray; // da znamo da cuvamo tip u nizu
        public VarType(BaseType type, boolean isArray) {
            this.type = type;
            this.isArray = isArray;
        }

        @Override
        protected String label() {
            return "VarType";
        }
        @Override
        protected String valueLabel() {
            return (isArray ? "xoArray " : "") + type.name();
        }
    }

    // ret_type   = var_type | "xoVoid" ;
    public static final class RetType extends NodeBase{
        public final boolean isVoid;
        public final VarType type;
        private RetType(boolean isVoid, VarType type) {
            this.isVoid = isVoid;
            this.type = type;
            add(type);  // dodajemo kao dete
        }

        // void, znaci nema povratnu vrednost
        public static RetType ofVoid() {
            return new RetType(true, null);
        }
        // imace povratnu vrednost tipa v
        public static RetType ofVar(VarType v) {
            return new RetType(false, v);
        }

        @Override
        protected String label() {
            return "RetType";
        }
        @Override
        protected String valueLabel() {
            return isVoid ? "xoVoid" : "";
        }
    }


    //param  = var_type IDENT ;
    public static final class Param extends NodeBase {
        public final VarType type;
        public final Token name; // promenljiva IDENT
        public Param(VarType type, Token name) {
            this.type = type;
            this.name = name;
            add(type);
        }
        @Override protected String label() { return "Param"; }
        @Override protected String valueLabel() { return type.valueLabel() + " " + name.lexeme; }
    }

    //block  = "{" { stmt } "}" ;
    public static final class Block extends Stmt {
        public final List<Stmt> stmts;
        public Block(List<Stmt> stmts) {
            this.stmts = stmts;
            addAll(stmts);
        }
        @Override protected String label() { return "Block"; }
    }

    //func_decl  = "CALLOUT" ret_type IDENT "(" param_list_opt ")" block ;
    public static final class FuncDecl extends NodeBase{
        public final RetType retType;
        public final Token name;          // IDENT ime funkcije
        public final List<Param> params;  // može biti prazno
        public final Block body;

        public FuncDecl(RetType retType, Token name, List<Param> params, Block body) {
            this.retType = retType; this.name = name; this.params = params; this.body = body;
            add(retType); addAll(params); add(body);
        }

        @Override protected String label() { return "FuncDecl"; }
        @Override protected String valueLabel() { return name.lexeme + " : " + (retType.isVoid ? "xoVoid" : retType.type.valueLabel()); }

    }


    // lvalue  = IDENT { "[" expr "]" } ;
    // PROMENLJIVA U KOJU DODELJUJEMO
    public static final class LValue extends NodeBase {
        public final Token name;          // IDENT - ime promenljive/niza
        public final List<Expr> indices;  // može biti prazno

        public LValue(Token name, List<Expr> indices) {
            this.name = name;
            this.indices = indices;
            addAll(indices); // svaki indeks je dete
        }

        @Override protected String label() { return "LValue"; }
        @Override protected String valueLabel() { return name.lexeme; }
    }

}
