package semantic;

import parser.ast.Ast;

import java.util.ArrayList;
import java.util.List;

public class FuncSymbol extends Symbol {

    public final Ast.RetType retType;
    public final List<Ast.Param> params;

    public FuncSymbol(String name, Ast.RetType retType, List<Ast.Param> params) {
        super(name);
        this.retType = retType;
        this.params = params;
    }

    public List<Ast.Param> getParams() {
        return params;
    }

    public Ast.RetType getRetType() {
        return retType;
    }
}
