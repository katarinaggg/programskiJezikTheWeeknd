package intermediate;

import parser.ast.Ast;
import parser.ast.Expr;
import parser.ast.Stmt;
import visitor.Visitor;

public class CodeGenerator implements Visitor {

    InstructionEmitter emitter;
    LabelManager labelManager;

    public CodeGenerator() {
        this.emitter = new InstructionEmitter();
        this.labelManager = new LabelManager();
    }

    public void generate(Ast.Program program) {
        program.accept(this);
    }

    public void writeToFile(String filename) {
        emitter.writeToFile(filename);
    }


    // VISIT METODE

    @Override
    public void visit(Expr.NumLit numLit) {
        emitter.emit("push " + numLit.value);
    }

    @Override
    public void visit(Expr.RealLit realLit) {
        emitter.emit("push " + realLit.value);
    }

    @Override
    public void visit(Expr.StringLit stringLit) {
        emitter.emit("push \"" + stringLit.value + "\""); //? proveriti
    }

    @Override
    public void visit(Expr.CharLit charLit) {
        emitter.emit("push " + (int)charLit.value);  //? proveriti
    }

    @Override
    public void visit(Expr.BoolLit boolLit) {
        emitter.emit("push " + (boolLit.value? 1 : 0));
    }

    @Override
    public void visit(Expr.Grouping grouping) {
        // prosledjujemo dalje unutrasnji expr
        grouping.inner.accept(this);
    }

    @Override
    public void visit(Expr.LValueRef lValueRef) {
        emitter.emit("push " + lValueRef.target.name.lexeme);
    }

    @Override
    public void visit(Expr.Unary unary) {
        // da se razresi expr ispred kojeg se nalazi operator
        unary.right.accept(this);

        switch (unary.op.lexeme){
            case "-" -> emitter.emit("neg");
            case "xoNot" -> emitter.emit("not");
            case "+" -> {} // nista
            default -> throw new RuntimeException("Nepoznat unary operator: " + unary.op.lexeme);
        }
    }

    @Override
    public void visit(Expr.Binary binary) {

        // razresavamo levu i desnu stranu operacije
        binary.left.accept(this);
        binary.right.accept(this);

        // operatori
        switch (binary.op.lexeme) {
            case "+"  -> emitter.emit("add");
            case "-"  -> emitter.emit("sub");
            case "*"  -> emitter.emit("mul");
            case "/"  -> emitter.emit("div");
            case "%"  -> emitter.emit("mod");

            case "<"  -> emitter.emit("lt");
            case ">"  -> emitter.emit("gt");
            case "<=" -> emitter.emit("le");
            case ">=" -> emitter.emit("ge");
            case "=" -> emitter.emit("eq");
            case "!=" -> emitter.emit("ne");

            case "xoAnd" -> emitter.emit("and");
            case "xoOr"  -> emitter.emit("or");

            default -> throw new RuntimeException("Nepoznat binarni operator: " + binary.op.lexeme);
        }
    }

    @Override
    public void visit(Ast.Program program) {

    }

    @Override
    public void visit(Ast.BlockMain blockMain) {

    }

    @Override
    public void visit(Ast.VarType varType) {

    }

    @Override
    public void visit(Ast.RetType retType) {

    }

    @Override
    public void visit(Ast.Param param) {

    }

    @Override
    public void visit(Ast.Block block) {

    }

    @Override
    public void visit(Ast.FuncDecl funcDecl) {

    }

    @Override
    public void visit(Ast.LValue lValue) {

    }


    @Override
    public void visit(Expr.UnaryStep unaryStep) {

    }


    @Override
    public void visit(Expr.ArrayLit arrayLit) {

    }

    @Override
    public void visit(Expr.Call call) {

    }

    @Override
    public void visit(Expr.Cast cast) {

    }

    @Override
    public void visit(Stmt.Return returnStmt) {

    }

    @Override
    public void visit(Stmt.VarDecl varDecl) {

    }

    @Override
    public void visit(Stmt.Assign assignStmt) {

    }

    @Override
    public void visit(Stmt.Listen listenStmt) {

    }

    @Override
    public void visit(Stmt.Echo echoStmt) {

    }

    @Override
    public void visit(Stmt.If ifStmt) {

    }

    @Override
    public void visit(Stmt.If.ElseIf elseIfStmt) {

    }

    @Override
    public void visit(Stmt.If.Else elseStmt) {

    }

    @Override
    public void visit(Stmt.RepeatAfterMe repeatAfterMe) {

    }

    @Override
    public void visit(Stmt.RepeatAfterMe.InitDecl initDecl) {

    }

    @Override
    public void visit(Stmt.RepeatAfterMe.InitAssign initAssign) {

    }

    @Override
    public void visit(Stmt.RepeatAfterMe.StepAssign stepAssign) {

    }

    @Override
    public void visit(Stmt.RepeatAfterMe.StepUnary stepUnary) {

    }

    @Override
    public void visit(Stmt.Timeless timeless) {

    }

    @Override
    public void visit(Stmt.CallStmt callStmt) {

    }
}
