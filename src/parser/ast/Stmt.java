package parser.ast;

import lexer.token.Token;

import java.util.List;

public abstract class Stmt extends NodeBase {


    // EXODUS [ expr ] ;
    public static final class Return extends Stmt {
        public final Token ex;    // EXODUS
        public final Expr expr;   // može biti null

        public Return(Token ex, Expr expr) {
            this.ex = ex; this.expr = expr;
            add(expr);
        }
        @Override protected String label() { return "Return"; }
        @Override
        protected String valueLabel() {
            return ex.lexeme;
        }
    }


    // CALLOUT var_type IDENT var_init_opt ;
    public static final class VarDecl extends Stmt {
        public final Token kw;
        public final Ast.VarType type;
        public final Token name;      // IDENT
        public final Expr init;       // može biti null (kad nema var_init_opt)

        public VarDecl(Token kw, Ast.VarType type, Token name, Expr init) {
            this.kw = kw; this.type = type; this.name = name; this.init = init;
            add(type, init);
        }

        @Override protected String label() { return "VarDecl"; }
        @Override protected String valueLabel() { return type.valueLabel() + " " + name.lexeme; }
    }

    // lvalue assign_op expr ;
    public static final class Assign extends Stmt {
        public final Ast.LValue target;
        public final Token op;        //assign ~>
        public final Expr value;

        public Assign(Ast.LValue target, Token op, Expr value) {
            this.target = target; this.op = op; this.value = value;
            add(target, value);
        }

        @Override protected String label() { return "Assign"; }
        @Override protected String valueLabel() { return op.lexeme; } // "~>"
    }

    // listen_stmt     = "LISTEN" "(" lvalue ")"
    public static final class Listen extends Stmt {
        public final Token kw;
        public final Ast.LValue target;

        public Listen(Token kw, Ast.LValue target) {
            this.kw = kw;
            this.target = target;
            add(target);
        }

        @Override protected String label() { return "Listen"; }
        @Override protected String valueLabel() { return kw.lexeme; }
    }


    // echo_stmt       = "ECHO" "(" expr ")" ;
    public static final class Echo extends Stmt {
        public final Token kw;
        public final Expr expr;

        public Echo(Token kw, Expr expr) {
            this.kw = kw;
            this.expr = expr;
            add(expr);
        }

        @Override protected String label() { return "Echo"; }
        @Override protected String valueLabel() { return kw.lexeme; }
    }


    // if_stmt = "tryMe" "(" expr ")" block
    // { "next" "(" expr ")" block }
    // [ "orNah" block ] ;
    public static final class If extends Stmt {

        public static final class ElseIf extends NodeBase {
            public final Expr condition;
            public final Ast.Block block;
            public ElseIf(Expr condition, Ast.Block block) {
                this.condition = condition; this.block = block;
                add(condition, block);
            }
            @Override protected String label() { return "ElseIf"; }
            @Override protected String valueLabel() { return "next"; }
        }

        public static final class Else extends NodeBase {
            public final Ast.Block block;
            public Else(Ast.Block block) { this.block = block; add(block); }
            @Override protected String label() { return "Else"; }
            @Override protected String valueLabel() { return "orNah"; }
        }

        public final Expr condition;
        public final Ast.Block thenBlock;
        public final List<ElseIf> elseIfs;       // moze da ima vise elseIf-ova
        public final Else elseBranch;            // opciono 1 else

        public If(Expr condition, Ast.Block thenBlock,
                  List<ElseIf> elseIfs, Else elseBranch) {
            this.condition = condition; this.thenBlock = thenBlock;
            this.elseIfs = elseIfs; this.elseBranch = elseBranch;
            add(condition, thenBlock);
            addAll(elseIfs);
            add(elseBranch);
        }

        @Override protected String label() { return "If"; }
        @Override protected String valueLabel() { return "tryMe"; }
    }


    // repeatAfterMe "(" for_init ";" for_cond_opt ";" for_step_opt ")" block
    public static final class RepeatAfterMe extends Stmt {

        // for_init = decl_for_init | assign_stmt | ε
        public static abstract class ForInit extends NodeBase {}

        // decl_for_init = "CALLOUT" var_type IDENT var_init_opt   (bez ';')
        public static final class InitDecl extends ForInit {
            public final Token kw;
            public final Ast.VarType type;
            public final Token name;
            public final Expr init;

            public InitDecl(Token kw, Ast.VarType type,
                            Token name, Expr init) {
                this.kw = kw; this.type = type; this.name = name; this.init = init;
                add(type, init);
            }
            @Override protected String label() { return "ForInitDecl"; }
            @Override protected String valueLabel() { return type.valueLabel() + " " + name.lexeme; }
        }

        // init kao dodela: assign_stmt (ali bez završnog ';' jer smo u header-u)
        public static final class InitAssign extends ForInit {
            public final Ast.LValue target;
            public final Token op;    // "~>"
            public final Expr value;
            public InitAssign(Ast.LValue target, Token op, Expr value) {
                this.target = target; this.op = op; this.value = value;
                add(target, value);
            }
            @Override protected String label() { return "ForInitAssign"; }
            @Override protected String valueLabel() { return op.lexeme; } // "~>"
        }

        // for_step_opt = ε | ( assign_stmt | unary_step )
        public static abstract class ForStep extends NodeBase {}

        // korak kao dodela
        public static final class StepAssign extends ForStep {
            public final Ast.LValue target;
            public final Token op;   // "~>"
            public final Expr value;
            public StepAssign(Ast.LValue target, Token op, Expr value) {
                this.target = target; this.op = op; this.value = value;
                add(target, value);
            }
            @Override protected String label() { return "ForStepAssign"; }
            @Override protected String valueLabel() { return op.lexeme; }
        }

        // korak kao rise/fade lvalue
        public static final class StepUnary extends ForStep {
            public final Expr.UnaryStep step;
            public StepUnary(Expr.UnaryStep step) { this.step = step; add(step); }
            @Override protected String label() { return "ForStepUnary"; }
        }

        public final ForInit init;
        public final Expr cond;
        public final ForStep step;
        public final Ast.Block body;

        public RepeatAfterMe(ForInit init, Expr cond, ForStep step, Ast.Block body) {
            this.init = init; this.cond = cond; this.step = step; this.body = body;
            add(init, cond, step, body);
        }

        @Override protected String label() { return "Loop"; }
        @Override protected String valueLabel() { return "repeatAfterMe"; }
    }

    // timeless "(" expr ")" block
    public static final class Timeless extends Stmt {
        public final Expr condition;
        public final Ast.Block body;
        public Timeless(Expr condition, Ast.Block body) {
            this.condition = condition; this.body = body;
            add(condition, body);
        }
        @Override protected String label() { return "Loop"; }
        @Override protected String valueLabel() { return "timeless"; }
    }


    // poziv funkcije samostalno (ne ubacujemo vrednost nigde)
    public static final class CallStmt extends Stmt {
        public final Expr.Call call;
        public CallStmt(Expr.Call call) { this.call = call; add(call); }
        @Override protected String label() { return "CallStmt"; }
        @Override protected String valueLabel() { return ((Expr.Call)call).name.lexeme; }
    }

}
