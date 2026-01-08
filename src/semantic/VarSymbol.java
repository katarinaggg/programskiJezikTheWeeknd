package semantic;

import parser.ast.Expr;
import parser.ast.Ast;

public class VarSymbol extends Symbol {

    private final Ast.VarType type;

    // inicijalizacioni izraz (ako postoji)
    private Expr initExpr;

    public VarSymbol(String name, Ast.VarType type) {
        super(name);
        this.type = type;
    }

    public Ast.VarType getType() {
        return type;
    }
    public void setInitExpr(Expr initExpr) {
        this.initExpr = initExpr;
    }
    public Expr getInitExpr() {
        return initExpr;
    }
}
