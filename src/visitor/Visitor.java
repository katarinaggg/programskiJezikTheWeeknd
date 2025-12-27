package visitor;

import parser.ast.Ast;
import parser.ast.Expr;
import parser.ast.Stmt;

public interface Visitor {

    void visit(Ast.Program program);
    void visit(Ast.BlockMain blockMain);
    void visit(Ast.VarType varType);
    void visit(Ast.RetType retType);
    void visit(Ast.Param param);
    void visit(Ast.Block block);
    void visit(Ast.FuncDecl funcDecl);
    void visit(Ast.LValue lValue);
    void visit(Expr.NumLit numLit);
    void visit(Expr.RealLit realLit);
    void visit(Expr.StringLit stringLit);
    void visit(Expr.CharLit charLit);
    void visit(Expr.BoolLit boolLit);
    void visit(Expr.Grouping grouping);
    void visit(Expr.Unary unary);
    void visit(Expr.UnaryStep unaryStep);
    void visit(Expr.Binary binary);
    void visit(Expr.LValueRef lValueRef);
    void visit(Expr.ArrayLit arrayLit);
    void visit(Expr.Call call);
    void visit(Expr.Cast cast);
    void visit(Stmt.Return returnStmt);
    void visit(Stmt.VarDecl varDecl);
    void visit(Stmt.Assign assignStmt);
    void visit(Stmt.Listen listenStmt);
    void visit(Stmt.Echo echoStmt);
    void visit(Stmt.If ifStmt);
    void visit(Stmt.If.ElseIf elseIfStmt);
    void visit(Stmt.If.Else elseStmt);
    void visit(Stmt.RepeatAfterMe repeatAfterMe);
    void visit(Stmt.RepeatAfterMe.InitDecl initDecl);
    void visit(Stmt.RepeatAfterMe.InitAssign initAssign);
    void visit(Stmt.RepeatAfterMe.StepAssign stepAssign);
    void visit(Stmt.RepeatAfterMe.StepUnary stepUnary);
    void visit(Stmt.Timeless timeless);
    void visit(Stmt.CallStmt callStmt);
}
