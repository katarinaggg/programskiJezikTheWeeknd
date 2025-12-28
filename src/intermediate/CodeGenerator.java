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
        // push referencu na niz / promenljivu
        emitter.emit("push " + lValueRef.target.name.lexeme);

        // za svaki indeks (svaka dimenzija)
        for (Expr idx : lValueRef.target.indices) {
            idx.accept(this);      // izračunaj indeks
            emitter.emit("array_get");
        }
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
        // funkcije pre main-a
        for (Ast.FuncDecl f : program.preMain) {
            f.accept(this);
        }

        // main
        program.main.accept(this);

        // funkcije posle main-a
        for (Ast.FuncDecl f : program.postMain) {
            f.accept(this);
        }
    }

    @Override
    public void visit(Ast.BlockMain blockMain) {
        emitter.emit("main:");
        for (Stmt stmt : blockMain.stmts) {
            stmt.accept(this);
        }
    }


    @Override
    public void visit(Ast.VarType varType) {
        // nema međukoda
    }

    @Override
    public void visit(Ast.RetType retType) {
        // nema međukoda
    }

    @Override
    public void visit(Ast.Param param) {
        // parametri se uzimaju sa steka
    }


    @Override
    public void visit(Ast.Block block) {
        for (Stmt stmt : block.stmts) {
            stmt.accept(this);
        }
    }

    @Override
    public void visit(Ast.FuncDecl funcDecl) {
        String funcLabel = funcDecl.name.lexeme;
        emitter.emit(funcLabel + ":");
        funcDecl.body.accept(this);
    }


    @Override
    public void visit(Ast.LValue lValue) {
        // koristi se u Assign / Listen / UnaryStep
    }

    @Override
    public void visit(Expr.UnaryStep unaryStep) {
        // ime promenljive
        String varName = unaryStep.target.name.lexeme;

        // učitaj trenutnu vrednost
        emitter.emit("push " + varName);
        emitter.emit("push 1");

        // rise = +1, fade = -1
        if (unaryStep.op.lexeme.equals("rise")) {
            emitter.emit("add");
        } else if (unaryStep.op.lexeme.equals("fade")) {
            emitter.emit("sub");
        } else {
            throw new RuntimeException("Nepoznat unary step operator: " + unaryStep.op.lexeme);
        }

        // upiši nazad u promenljivu
        emitter.emit("pop " + varName);
    }

    @Override
    public void visit(Expr.ArrayLit arrayLit) {
        int n = arrayLit.elements.size();

        for (Expr e : arrayLit.elements) {
            e.accept(this);
        }

        emitter.emit("new_array " + n);
    }


    @Override
    public void visit(Expr.Call call) {
        // argumenti
        for (Expr arg : call.args) {
            arg.accept(this);
        }

        String retLabel = labelManager.newLabel("RET");
        emitter.emit("push " + retLabel);
        emitter.emit("jmp " + call.name.lexeme);
        emitter.emit(retLabel + ":");
    }


    @Override
    public void visit(Expr.Cast cast) {
        cast.expr.accept(this);
        emitter.emit("cast_" + cast.targetType.toString().toLowerCase());
    }


    @Override
    public void visit(Stmt.Return returnStmt) {
        if (returnStmt.expr != null) {
            returnStmt.expr.accept(this);
        }
        emitter.emit("pop IP");
        emitter.emit("jmp IP");
    }


    @Override
    public void visit(Stmt.VarDecl varDecl) {
        // ako postoji inicijalizacija
        if (varDecl.init != null) {
            // izračunaj izraz (vrednost ostaje na steku)
            varDecl.init.accept(this);

            // upiši vrednost u promenljivu
            emitter.emit("pop " + varDecl.name.lexeme);
        }
        // ako nema init-a → nema međukoda
    }

    @Override
    public void visit(Stmt.Assign assignStmt) {
        // izračunaj vrednost (ostavlja rezultat na steku)
        assignStmt.value.accept(this);

        // upiši u promenljivu
        emitter.emit("pop " + assignStmt.target.name.lexeme);
    }

    @Override
    public void visit(Stmt.Listen listenStmt) {
        emitter.emit("listen " + listenStmt.target.name.lexeme);
    }

    @Override
    public void visit(Stmt.Echo echoStmt) {
        echoStmt.expr.accept(this);
        emitter.emit("echo");
    }


    @Override
    public void visit(Stmt.If ifStmt) {
        String endLabel = labelManager.newLabel("IF_END");
        String nextLabel;

        // IF
        ifStmt.condition.accept(this);
        nextLabel = labelManager.newLabel("IF_NEXT");
        emitter.emit("jmp_zero " + nextLabel);
        ifStmt.thenBlock.accept(this);
        emitter.emit("jmp " + endLabel);
        emitter.emit(nextLabel + ":");

        // ELSE IF*
        for (Stmt.If.ElseIf ei : ifStmt.elseIfs) {
            ei.condition.accept(this);
            String skip = labelManager.newLabel("ELSEIF_NEXT");
            emitter.emit("jmp_zero " + skip);
            ei.block.accept(this);
            emitter.emit("jmp " + endLabel);
            emitter.emit(skip + ":");
        }

        // ELSE
        if (ifStmt.elseBranch != null) {
            ifStmt.elseBranch.block.accept(this);
        }

        emitter.emit(endLabel + ":");
    }

    @Override
    public void visit(Stmt.If.ElseIf elseIfStmt) {
        // obrađuje se u visit(Stmt.If)
    }

    @Override
    public void visit(Stmt.If.Else elseStmt) {
        // obrađuje se u visit(Stmt.If)
    }


    @Override
    public void visit(Stmt.RepeatAfterMe repeatAfterMe) {
        String start = labelManager.newLabel("LOOP_START");
        String end   = labelManager.newLabel("LOOP_END");

        if (repeatAfterMe.init != null) {
            repeatAfterMe.init.accept(this);
        }

        emitter.emit(start + ":");

        if (repeatAfterMe.cond != null) {
            repeatAfterMe.cond.accept(this);
            emitter.emit("jmp_zero " + end);
        }

        repeatAfterMe.body.accept(this);

        if (repeatAfterMe.step != null) {
            repeatAfterMe.step.accept(this);
        }

        emitter.emit("jmp " + start);
        emitter.emit(end + ":");
    }


    @Override
    public void visit(Stmt.RepeatAfterMe.InitDecl initDecl) {
        if (initDecl.init != null) {
            initDecl.init.accept(this);
            emitter.emit("pop " + initDecl.name.lexeme);
        }
    }


    @Override
    public void visit(Stmt.RepeatAfterMe.InitAssign initAssign) {
        initAssign.value.accept(this);
        emitter.emit("pop " + initAssign.target.name.lexeme);
    }


    @Override
    public void visit(Stmt.RepeatAfterMe.StepAssign stepAssign) {
        stepAssign.value.accept(this);
        emitter.emit("pop " + stepAssign.target.name.lexeme);
    }


    @Override
    public void visit(Stmt.RepeatAfterMe.StepUnary stepUnary) {
        stepUnary.step.accept(this);
    }


    @Override
    public void visit(Stmt.Timeless timeless) {
        String start = labelManager.newLabel("WHILE_START");
        String end   = labelManager.newLabel("WHILE_END");

        emitter.emit(start + ":");
        timeless.condition.accept(this);
        emitter.emit("jmp_zero " + end);

        timeless.body.accept(this);

        emitter.emit("jmp " + start);
        emitter.emit(end + ":");
    }


    @Override
    public void visit(Stmt.CallStmt callStmt) {
        callStmt.call.accept(this);
        emitter.emit("pop"); // odbacuje povratnu vrednost
    }

    @Override
    public void visit(Stmt.UnaryStepStmt stmt) {
        stmt.step.accept(this);
    }

}
