package parser.ast;

import java.util.List;
import java.util.Objects;

import lexer.token.Token;
import visitor.Visitor;

public final class Ast {

    //  KOREN PROGRAMA

    // { func_decl } "XOTWOD" block_main { func_decl } EOF ;
    public static final class Program extends NodeBase{
        public final List<FuncDecl> preMain;
        public final BlockMain main;
        public final List<FuncDecl> postMain;
        public int mainCount = 0;

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

        @Override
        public void accept(Visitor v) {
            v.visit(this);
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

        @Override
        public void accept(Visitor v) {
            v.visit(this);
        }
    }


    // base_type  = "xoNum" | "xoDouble" | "xoChar" | "xoBool" | "xoString" ;
    public enum BaseType {XONUM, XODOUBLE, XOCHAR, XOBOOL, XOSTRING}


    //var_type  = base_type | "xoArray" base_type ;
    /*
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

        @Override
        public void accept(Visitor v) {
            v.visit(this);
        }
    }
    */

    //var_type  = base_type | "xoArray" var_type ;
    public static final class VarType extends NodeBase {

        // ako je prost tip
        public final BaseType baseType;

        // ako je niz
        public final VarType elementType;

        // prost tip (xoNum, xoBool, ...)
        public VarType(BaseType baseType) {
            this.baseType = baseType;
            this.elementType = null;
        }

        // niz tipa elementType
        public VarType(VarType elementType) {
            this.baseType = null;
            this.elementType = elementType;
        }

        public boolean isArray() {
            return elementType != null;
        }

        public VarType getElementType() {
            return elementType;
        }

        public BaseType getBaseType() {
            if (isArray()) {
                return elementType.getBaseType();
            }
            return baseType;
        }

        @Override
        protected String label() {
            return "VarType";
        }

        @Override
        protected String valueLabel() {
            if (isArray()) {
                return "xoArray " + elementType.valueLabel();
            }
            return baseType.name();
        }

        @Override
        public void accept(Visitor v) {
            v.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VarType other)) return false;

            // jedan je niz, drugi nije
            if (this.isArray() != other.isArray()) return false;

            // oba su nizovi → poredi elemente
            if (this.isArray()) {
                return this.elementType.equals(other.elementType);
            }

            // oba su prosti tipovi
            return this.baseType == other.baseType;
        }

        @Override
        public int hashCode() {
            if (isArray()) {
                return Objects.hash("ARRAY", elementType);
            }
            return Objects.hash("BASE", baseType);
        }

        @Override
        public String toString() {
            if (isArray()) {
                return elementType.toString() + "[]";
            }
            return baseType.name();
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

        @Override
        public void accept(Visitor v) {
            v.visit(this);
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

        @Override
        public void accept(Visitor v) {
            v.visit(this);
        }
    }

    //block  = "{" { stmt } "}" ;
    public static final class Block extends Stmt {
        public final List<Stmt> stmts;
        public Block(List<Stmt> stmts) {
            this.stmts = stmts;
            addAll(stmts);
        }
        @Override protected String label() { return "Block"; }

        @Override
        public void accept(Visitor v) {
            v.visit(this);
        }
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

        @Override
        public void accept(Visitor v) {
            v.visit(this);
        }
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

        @Override
        public void accept(Visitor v) {
            v.visit(this);
        }
    }

}
