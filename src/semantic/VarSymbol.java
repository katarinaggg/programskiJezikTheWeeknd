package semantic;

import parser.ast.Ast;

public class VarSymbol extends Symbol {

    public final Ast.VarType type;

    public VarSymbol(String name,Ast.VarType type) {
        super(name);
        this.type = type;
    }

    public Ast.VarType getType() {
        return type;
    }
}
