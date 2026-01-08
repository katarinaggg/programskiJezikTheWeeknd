package intermediate;

import parser.ast.Ast;
import parser.ast.Expr;
import parser.ast.Stmt;
import visitor.Visitor;
public class CodeGenerator implements Visitor {

    // Emitter skuplja instrukcije i kasnije ih upisuje u fajl
    InstructionEmitter emitter;

    // LabelManager pravi jedinstvene labele
    // (IF_END, LOOP_START, RET, itd.)
    LabelManager labelManager;

    // Flag koji oznacava da li smo u main funkciji
    // koristi se da se return u main-u ignorise
    private boolean inMain = false;

    // Flag koji pamti da li funkcija ima eksplicitan return
    // ako nema, na kraju funkcije se radi jmp IP
    private boolean functionHasReturn = false;


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
            idx.accept(this);      // izracunaj indeks
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

        emitter.emit("jmp MAIN");

        for (Ast.FuncDecl f : program.preMain) {
            f.accept(this);
        }
        for (Ast.FuncDecl f : program.postMain) {
            f.accept(this);
        }

        emitter.emitLabel("MAIN");
        program.main.accept(this);
    }


    @Override
    public void visit(Ast.BlockMain blockMain) {
        inMain = true;

        for (Stmt stmt : blockMain.stmts) {
            stmt.accept(this);
        }

        inMain = false;
    }

    @Override
    public void visit(Ast.VarType varType) {
        // nema medjukoda
    }

    @Override
    public void visit(Ast.RetType retType) {
        // nema medjukoda
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

        String skipFunc = labelManager.newLabel("SKIP_FUNC");
        emitter.emit("jmp " + skipFunc);

        emitter.emitLabel(funcDecl.name.lexeme);

        emitter.emit("pop IP");

        functionHasReturn = false;

        for (int i = funcDecl.params.size() - 1; i >= 0; i--) {
            emitter.emit("pop " + funcDecl.params.get(i).name.lexeme);
        }

        funcDecl.body.accept(this);

        // SAMO AKO NIJE BILO return-a
        if (!functionHasReturn) {
            emitter.emit("jmp IP");
        }

        emitter.emitLabel(skipFunc);
    }


    @Override
    public void visit(Ast.LValue lValue) {
        // koristi se u Assign / Listen / UnaryStep
    }

    @Override
    public void visit(Expr.UnaryStep unaryStep) {
        // ime promenljive
        String varName = unaryStep.target.name.lexeme;

        // ucitaj trenutnu vrednost
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

        // upisi nazad u promenljivu
        emitter.emit("pop " + varName);
        // nova vrednost je i REZULTAT IZRAZA
        emitter.emit("push " + varName);
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

        // 1. argumenti
        for (Expr arg : call.args) {
            arg.accept(this);
        }

        // 2. povratna adresa
        String retLabel = labelManager.newLabel("RET");
        emitter.emit("push " + retLabel);

        // 3. skok NA LABELU FUNKCIJE
        emitter.emit("jmp " + call.name.lexeme);

        // 4. povratak
        emitter.emitLabel(retLabel);

    }

    @Override
    public void visit(Expr.Cast cast) {
        cast.expr.accept(this);
        emitter.emit("cast_" + cast.targetType.toString().toLowerCase());
    }
    @Override
    public void visit(Stmt.Return returnStmt) {

        if (inMain) {
            return;   // return u main-u se ignorise
        }

        if (returnStmt.expr != null) {
            returnStmt.expr.accept(this);
        }

        functionHasReturn = true;
        emitter.emit("jmp IP");
    }

    @Override
    public void visit(Stmt.VarDecl varDecl) {
        // ako postoji inicijalizacija
        if (varDecl.init != null) {
            // izracunaj izraz (vrednost ostaje na steku)
            varDecl.init.accept(this);

            // upisi vrednost u promenljivu
            emitter.emit("pop " + varDecl.name.lexeme);
        }
        // ako nema init-a onda nema medjukoda
    }

    @Override
    public void visit(Stmt.Assign assignStmt) {
        // izracunaj vrednost (ostavlja rezultat na steku)
        assignStmt.value.accept(this);

        // upisi u promenljivu
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
        String ifFalse = labelManager.newLabel("IF_FALSE");

        // IF
        ifStmt.condition.accept(this);
        emitter.emit("jmp_zero " + ifFalse);

        ifStmt.thenBlock.accept(this);
        emitter.emit("jmp " + endLabel);

        // IF_FALSE
        emitter.emitLabel(ifFalse);

        // ELSE IF*
        for (Stmt.If.ElseIf ei : ifStmt.elseIfs) {

            String elseIfFalse = labelManager.newLabel("ELSEIF_FALSE");

            ei.condition.accept(this);
            emitter.emit("jmp_zero " + elseIfFalse);

            ei.block.accept(this);
            emitter.emit("jmp " + endLabel);

            emitter.emitLabel(elseIfFalse);
        }

        // ELSE (orNah)
        if (ifStmt.elseBranch != null) {
            ifStmt.elseBranch.block.accept(this);
        }

        // END — tacka izlaza
        emitter.emitLabel(endLabel);
    }


    @Override
    public void visit(Stmt.If.ElseIf elseIfStmt) {
        // obradjuje se u visit(Stmt.If)
    }

    @Override
    public void visit(Stmt.If.Else elseStmt) {
        // obradjuje se u visit(Stmt.If)
    }

    @Override
    public void visit(Stmt.RepeatAfterMe repeatAfterMe) {

        String startLabel = labelManager.newLabel("LOOP_START");
        String endLabel   = labelManager.newLabel("LOOP_END");

        if (repeatAfterMe.init != null) {
            repeatAfterMe.init.accept(this);
        }

        emitter.emitLabel(startLabel);


        if (repeatAfterMe.cond != null) {
            repeatAfterMe.cond.accept(this);
            emitter.emit("jmp_zero " + endLabel);
        }

        repeatAfterMe.body.accept(this);

        if (repeatAfterMe.step != null) {
            repeatAfterMe.step.accept(this);
        }

        emitter.emit("jmp " + startLabel);

        emitter.emitLabel(endLabel);
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

        // LABELA = pocetak uslova
        emitter.emitLabel(start);

        // USLOV
        timeless.condition.accept(this);   // ostavlja bool na steku
        emitter.emit("jmp_zero " + end);   // ako false onda kraj petlje

        // TELO PETLJE
        timeless.body.accept(this);

        // POVRATAK NA USLOV OBAVEZNO
        emitter.emit("jmp " + start);

        // KRAJ PETLJE
        emitter.emitLabel(end);
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
