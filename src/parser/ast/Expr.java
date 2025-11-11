package parser.ast;

import lexer.token.Token;

import java.util.List;

public abstract class Expr extends NodeBase {

    // primary elementi

    // NUMBER
    public static final class NumLit extends Expr {
        public final Token token;
        public final int value;
        public NumLit(Token token, int value) { this.token = token; this.value = value; }
        @Override protected String label() { return "NumLit"; }
        @Override protected String valueLabel() { return String.valueOf(value); }
    }

    // DOUBLE
    public static final class RealLit extends Expr {
        public final Token token;
        public final double value;
        public RealLit(Token token, double value) { this.token = token; this.value = value; }
        @Override protected String label() { return "RealLit"; }
        @Override protected String valueLabel() { return String.valueOf(value); }
    }

    // STRING
    public static final class StringLit extends Expr {
        public final Token token;
        public final String value;
        public StringLit(Token token, String value) { this.token = token; this.value = value; }
        @Override protected String label() { return "StringLit"; }
        @Override protected String valueLabel() { return value; }
    }

    // CHAR
    public static final class CharLit extends Expr {
        public final Token token;
        public final char value;
        public CharLit(Token token, char value) { this.token = token; this.value = value; }
        @Override protected String label() { return "CharLit"; }
        @Override protected String valueLabel() { return String.valueOf(value); }
    }

    // TRUE/FALSE
    public static final class BoolLit extends Expr {
        public final Token token;
        public final boolean value;
        public BoolLit(Token token, boolean value) { this.token = token; this.value = value; }
        @Override protected String label() { return "BoolLit"; }
        @Override protected String valueLabel() { return String.valueOf(value); }
    }


    // ( expr ) u zagradama
    public static final class Grouping extends Expr {
        public final Expr inner;
        public Grouping(Expr inner) { this.inner = inner; add(inner); }
        @Override protected String label() { return "Grouping"; }
    }


    public static final class Unary extends Expr {
        public final Token op; // "+", "-", "xoNot"
        public final Expr right;
        public Unary(lexer.token.Token op, Expr right) { this.op = op; this.right = right; add(right); }
        @Override protected String label() { return "Unary"; }
        @Override protected String valueLabel() { return op.lexeme; }
    }

    // unary_step    = ( "rise" | "fade" ) lvalue ;
    public static final class UnaryStep extends Expr {
        public final Token op;       // rise ili fade
        public final Ast.LValue target;

        public UnaryStep(Token op, Ast.LValue target) {
            this.op = op;
            this.target = target;
            add(target);
        }
        @Override protected String label() { return "UnaryStep"; }
        @Override protected String valueLabel() { return op.lexeme; }
    }

    public static final class Binary extends Expr {
        public final Expr left;
        public final Token op; // "+", "-", "*", "/", "%", "=", "!=", "<", ">", "<=", ">=", "xoAnd", "xoOr"
        public final Expr right;
        public Binary(Expr left, lexer.token.Token op, Expr right) {
            this.left = left; this.op = op; this.right = right;
            add(left, right);
        }
        @Override protected String label() { return "Binary"; }
        @Override protected String valueLabel() { return op.lexeme; }
    }


    // VREDNOST U LVALUE
    public static final class LValueRef extends Expr {
        public final Ast.LValue target;

        public LValueRef(Ast.LValue target) {
            this.target = target;
            add(target);
        }
        @Override protected String label() { return "LValueRef"; }
        @Override protected String valueLabel() { return target.name.lexeme; }
    }

    // array_lit = "[" [ array_element { "," array_element } ] "]"
    // array_element = expr | array_lit
    public static final class ArrayLit extends Expr {
        public final List<Expr> elements;

        public ArrayLit(java.util.List<Expr> elements) {
            this.elements = elements;
            addAll(elements);
        }
        @Override protected String label() { return "ArrayLit"; }
        @Override protected String valueLabel() { return "size=" + (elements == null ? 0 : elements.size()); }
    }


    // poziv funkcije
    public static final class Call extends Expr {
        public final Token name;     // IDENT - ime funkcije
        public final List<Expr> args;
        public Call(Token name, java.util.List<Expr> args) {
            this.name = name; this.args = args;
            addAll(args);
        }
        @Override protected String label() { return "Call"; }
        @Override protected String valueLabel() { return name.lexeme; }
    }
}
