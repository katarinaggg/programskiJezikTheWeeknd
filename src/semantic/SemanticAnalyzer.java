package semantic;

import lexer.token.Token;
import parser.ast.Ast;
import parser.ast.Expr;
import parser.ast.NodeBase;
import parser.ast.Stmt;
import visitor.Visitor;

import java.util.ArrayList;
import java.util.List;

public class SemanticAnalyzer implements Visitor {

    // trenutni blok vidljivosti
    private Scope currentScope;

    // lista semantickih gresaka koje ce se ispisati
    private final List<SemanticError> errors = new ArrayList<>();

    // pamtimo povratnu vr trenutne funkcije koju analiziramo
    private Ast.RetType currentFunctionReturnType = null;


    public void analyze(Ast.Program program) {

        // globalni scope
        currentScope = new Scope(null);

        //  FAZA 1
        // koje funkcije postoje (mogu i pre i posle main-a)
        // main i provera duplikata main-a
        registerFunctions(program);
        checkMainFunction(program);

        //  FAZA 2
        // obilazak AST-a pomocu visitora
        program.accept(this);

        reportResults();
    }


    private void registerFunctions(Ast.Program program) {

        // funkcije pre main-a
        for (Ast.FuncDecl func : program.preMain) {
            registerFunction(func);
        }

        // main funkcija
        if (program.main != null) {
            // vraca void, nema argumente
            FuncSymbol mainSymbol = new FuncSymbol("main", Ast.RetType.ofVoid(), List.of());
            if (!currentScope.containsLocal("main")) {
                currentScope.define(mainSymbol);
            }
        }

        // funkcije posle main-a
        for (Ast.FuncDecl func : program.postMain) {
            registerFunction(func);
        }
    }


    private void registerFunction(Ast.FuncDecl func) {

        String name = func.name.lexeme;

        if (currentScope.containsLocal(name)) {
            reportError(
                    SemanticError.Type.DUPLICATE_SYMBOL,
                    func,
                    func.name,
                    "function '" + name + "'"
            );
            return;
        }
        FuncSymbol symbol = new FuncSymbol(name, func.retType, func.params);
        currentScope.define(symbol);
    }



    private void checkMainFunction(Ast.Program program) {
        if (program.mainCount == 0) {
            reportError(
                    SemanticError.Type.NO_MAIN_FUNCTION,
                    null,
                    null,
                    null
            );
        } else if (program.mainCount > 1) {
            reportError(
                    SemanticError.Type.MULTIPLE_MAIN_FUNCTIONS,
                    null,
                    null,
                    null
            );
        }
    }


    private void reportError(SemanticError.Type type, NodeBase node, Token token, String details) {
        errors.add(new SemanticError(type, node, token, details));
    }

    private void reportResults(){
        if(!errors.isEmpty()){
            for(SemanticError error : errors){
                System.err.println(error.format());
            }
        }
    }


    // pravimo novi scope -> to je sada trenutni
    private void enterScope() {
        currentScope = new Scope(currentScope);
    }

    // izlazimo iz trenutnog -> vracamo se na njegovog roditelja (prethodni)
    private void exitScope() {
        currentScope = currentScope.getParent();
    }


    /*
        VISIT METODE
    */

    @Override
    public void visit(Ast.Program program) {

        if (program.main != null) {
            program.main.accept(this);
        }

        // fje pre main-a
        for (Ast.FuncDecl func : program.preMain) {
            func.accept(this);
        }

        // fje posle main-a
        for (Ast.FuncDecl func : program.postMain) {
            func.accept(this);
        }
    }


    @Override
    public void visit(Ast.FuncDecl funcDecl) {

        // ulazimo u scope fje
        enterScope();

        Ast.RetType previousReturnType = currentFunctionReturnType;
        currentFunctionReturnType = funcDecl.retType;

        // prolazimo kroz argumente(parametre)
        for (Ast.Param param : funcDecl.params) {

            String paramName = param.name.lexeme;

            // duplikat parametra
            if (currentScope.containsLocal(paramName)) {
                reportError(
                        SemanticError.Type.DUPLICATE_SYMBOL,
                        param,
                        param.name,
                        "parameter '" + paramName + "'"
                );
                continue;
            }

            VarSymbol paramSymbol = new VarSymbol(
                    paramName,
                    param.type
            );

            currentScope.define(paramSymbol);
        }

        // obilazimo telo fje
        funcDecl.body.accept(this);

        // vracamo prethodni kontekst
        currentFunctionReturnType = previousReturnType;

        // izlazimo iz scope-a fje
        exitScope();
    }


    @Override
    public void visit(Ast.Block block) {

        // ulazimo u novi
        enterScope();

        // obilazimo sve statemente iz blocka
        for (Stmt stmt : block.stmts) {
            stmt.accept(this);
        }

        // vracamo se na stari scope (kad izlazimo iz trenutnog)
        exitScope();
    }


    @Override
    public void visit(Stmt.VarDecl varDecl) {
        String name = varDecl.name.lexeme;

        // duplikat
        if (currentScope.containsLocal(name)) {
            reportError(
                    SemanticError.Type.DUPLICATE_SYMBOL,
                    varDecl,
                    varDecl.name,
                    "variable '" + name + "'"
            );
            return;
        }

        // stavljamo je u scope
        VarSymbol symbol = new VarSymbol(name, varDecl.type);
        currentScope.define(symbol);

        // inicijalizacija (ako postoji)
        if (varDecl.init != null) {

            // prvo analiziramo izraz
            varDecl.init.accept(this);

            // dobijamo njegov tip
            Ast.VarType tipIzraza = varDecl.init.inferredType;

            // proveravamo tipove
            if(tipIzraza != null && !tipIzraza.equals(varDecl.type)) {
                reportError(
                        SemanticError.Type.INVALID_ASSIGNMENT,
                        varDecl,
                        varDecl.name,
                        "cannot assign " + tipIzraza +
                                " to variable of type " + varDecl.type
                );
            }
        }
    }


    @Override
    public void visit(Expr.LValueRef lValueRef) {
        // ovde je zapravo kako koristimo lvalue
        // lvalue je samo struktura, adresa
        // lvalueref je njena prava vrednost, izraz

        String name = lValueRef.target.name.lexeme;

        // pronalazak simbola
        Symbol symbol = currentScope.resolve(name);

        if (symbol == null || !(symbol instanceof VarSymbol)) {
            reportError(
                    SemanticError.Type.UNDECLARED_SYMBOL,
                    lValueRef,
                    lValueRef.target.name,
                    "variable '" + name + "'"
            );
            lValueRef.inferredType = null;
            return;
        }

        // pocetni tip (tip promenljive)
        Ast.VarType currentType = ((VarSymbol) symbol).getType();

        // indeksiranje (ako postoji)
        for (Expr indexExpr : lValueRef.target.indices) {

            // mora biti niz
            if (!currentType.isArray()) {
                reportError(
                        SemanticError.Type.INVALID_INDEXING,
                        lValueRef,
                        lValueRef.target.name,
                        "type '" + currentType + "' is not indexable"
                );
                lValueRef.inferredType = null;
                return;
            }

            // obrada indeksa
            indexExpr.accept(this);

            // indeks mora biti broj
            if (indexExpr.inferredType != null &&
                    indexExpr.inferredType.getBaseType() != Ast.BaseType.XONUM) {

                reportError(
                        SemanticError.Type.INVALID_INDEXING,
                        indexExpr,
                        lValueRef.target.name,
                        "array index must be a number"
                );
            }

            // novi tip = element niza
            currentType = currentType.getElementType();
        }

        //  konacni tip lvalue-a
        lValueRef.inferredType = currentType;
    }


    @Override
    public void visit(Stmt.Assign assignStmt) {

        // leva strana
        Expr.LValueRef lvalueRef = new Expr.LValueRef(assignStmt.target);

        // da bi izvukli tip
        lvalueRef.accept(this);
        Ast.VarType leftType = lvalueRef.inferredType;

        if (leftType == null) {
            return;
        }

        // desna strana (izraz) -> izvlacimo tip
        assignStmt.value.accept(this);
        Ast.VarType rightType = assignStmt.value.inferredType;

        if (rightType == null) {
            return;
        }

        // provera kompatibilnosti tipova
        if (!areTypesAssignable(leftType, rightType)) {
            reportError(
                    SemanticError.Type.INVALID_ASSIGNMENT,
                    assignStmt,
                    assignStmt.target.name,
                    "cannot assign " + rightType + " to " + leftType
            );
        }
    }

    private boolean areTypesAssignable(Ast.VarType left, Ast.VarType right) {

        // niz i neniz
        if (left.isArray() != right.isArray()) {
            return false;
        }

        // oba su nizovi -> proveravamo elemente
        if (left.isArray()) {
            return areTypesAssignable(
                    left.getElementType(),
                    right.getElementType()
            );
        }

        // prosti tipovi
        return left.getBaseType() == right.getBaseType();
    }


    @Override
    public void visit(Expr.Binary binary) {

        // analiziramo levi i desni operand
        binary.left.accept(this);
        binary.right.accept(this);

        // izvlacimo im tipove
        Ast.VarType leftType = binary.left.inferredType;
        Ast.VarType rightType = binary.right.inferredType;

        if (leftType == null || rightType == null) {
            binary.inferredType = null;
            return;
        }

        switch (binary.op.type) {

            // ARITMETIČKI OPERATORI

            case ADD:
            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case PERCENT: {

                // ne smeju biti nizovi
                if (leftType.isArray() || rightType.isArray()) {
                    reportError(
                            SemanticError.Type.ARITHMETIC_TYPE_MISMATCH,
                            binary,
                            binary.op,
                            "arrays cannot be used in arithmetic expressions"
                    );
                    binary.inferredType = null;
                    return;
                }

                Ast.BaseType l = leftType.getBaseType();
                Ast.BaseType r = rightType.getBaseType();

                // rpoveravamo da li su brojevi
                boolean lNum = (l == Ast.BaseType.XONUM || l == Ast.BaseType.XODOUBLE);
                boolean rNum = (r == Ast.BaseType.XONUM || r == Ast.BaseType.XODOUBLE);

                // ako nisu brojevi ili ako nisu istog tipa
                if (!lNum || !rNum || l != r) {
                    reportError(
                            SemanticError.Type.ARITHMETIC_TYPE_MISMATCH,
                            binary,
                            binary.op,
                            "arithmetic operands must be both xoNum or both xoDouble"
                    );
                    binary.inferredType = null;
                    return;
                }

                // postavljamo tip izraza
                binary.inferredType = new Ast.VarType(l);
                return;
            }


            // RELACIONI OPERATORI

            case EQ:
            case NEQ:
            case LT:
            case GT:
            case LE:
            case GE: {

                if (leftType.isArray() || rightType.isArray()) {
                    reportError(
                            SemanticError.Type.RELATIONAL_OPERAND_NOT_NUMBER,
                            binary,
                            binary.op,
                            "arrays cannot be used in relational expressions"
                    );
                    binary.inferredType = null;
                    return;
                }

                Ast.BaseType l = leftType.getBaseType();
                Ast.BaseType r = rightType.getBaseType();

                boolean lNum = (l == Ast.BaseType.XONUM || l == Ast.BaseType.XODOUBLE);
                boolean rNum = (r == Ast.BaseType.XONUM || r == Ast.BaseType.XODOUBLE);

                if (!lNum || !rNum || l != r) {
                    reportError(
                            SemanticError.Type.RELATIONAL_OPERAND_NOT_NUMBER,
                            binary,
                            binary.op,
                            "relational operands must be numbers of the same type"
                    );
                    binary.inferredType = null;
                    return;
                }

                // rezultat je uvek bool
                binary.inferredType = new Ast.VarType(Ast.BaseType.XOBOOL);
                return;
            }


            // LOGIČKI OPERATORI

            case XOAND:
            case XOOR: {

                if (leftType.isArray() || rightType.isArray()) {
                    reportError(
                            SemanticError.Type.LOGICAL_OPERAND_NOT_BOOLEAN,
                            binary,
                            binary.op,
                            "arrays cannot be used in logical expressions"
                    );
                    binary.inferredType = null;
                    return;
                }

                if (leftType.getBaseType() != Ast.BaseType.XOBOOL ||
                        rightType.getBaseType() != Ast.BaseType.XOBOOL) {

                    reportError(
                            SemanticError.Type.LOGICAL_OPERAND_NOT_BOOLEAN,
                            binary,
                            binary.op,
                            "logical operands must be boolean"
                    );
                    binary.inferredType = null;
                    return;
                }

                binary.inferredType = new Ast.VarType(Ast.BaseType.XOBOOL);
                return;
            }

            default:
                binary.inferredType = null;
        }
    }


    @Override
    public void visit(Expr.Unary unary) {
        // prvo analiziramo operand
        unary.right.accept(this);
        Ast.VarType rightType = unary.right.inferredType;

        if (rightType == null) {
            unary.inferredType = null;
            return;
        }

        switch (unary.op.type) {

            // +expr / -expr

            case ADD:
            case SUBTRACT: {

                // ne sme biti niz
                if (rightType.isArray()) {
                    reportError(
                            SemanticError.Type.ARITHMETIC_TYPE_MISMATCH,
                            unary,
                            unary.op,
                            "arrays cannot be used with unary arithmetic operators"
                    );
                    unary.inferredType = null;
                    return;
                }

                Ast.BaseType bt = rightType.getBaseType();

                if (bt != Ast.BaseType.XONUM && bt != Ast.BaseType.XODOUBLE) {
                    reportError(
                            SemanticError.Type.ARITHMETIC_TYPE_MISMATCH,
                            unary,
                            unary.op,
                            "unary + and - require numeric operand"
                    );
                    unary.inferredType = null;
                    return;
                }

                // tip ostaje isti
                unary.inferredType = rightType;
                return;
            }


            // xoNot expr

            case XONOT: {

                if (rightType.isArray() ||
                        rightType.getBaseType() != Ast.BaseType.XOBOOL) {

                    reportError(
                            SemanticError.Type.LOGICAL_OPERAND_NOT_BOOLEAN,
                            unary,
                            unary.op,
                            "xoNot requires boolean operand"
                    );
                    unary.inferredType = null;
                    return;
                }

                unary.inferredType = new Ast.VarType(Ast.BaseType.XOBOOL);
                return;
            }

            default:
                unary.inferredType = null;
        }
    }

    @Override
    public void visit(Expr.UnaryStep unaryStep) {

        // pravimo referencu da izvucemo tip
        Expr.LValueRef ref = new Expr.LValueRef(unaryStep.target);
        ref.accept(this);

        Ast.VarType type = ref.inferredType;

        if (type == null) {
            unaryStep.inferredType = null;
            return;
        }

        // ne sme biti niz
        if (type.isArray()) {
            reportError(
                    SemanticError.Type.ARITHMETIC_TYPE_MISMATCH,
                    unaryStep,
                    unaryStep.op,
                    "rise/fade cannot be applied to arrays"
            );
            unaryStep.inferredType = null;
            return;
        }

        Ast.BaseType bt = type.getBaseType();

        // mora biti broj
        if (bt != Ast.BaseType.XONUM && bt != Ast.BaseType.XODOUBLE) {
            reportError(
                    SemanticError.Type.ARITHMETIC_TYPE_MISMATCH,
                    unaryStep,
                    unaryStep.op,
                    "rise/fade require numeric operand"
            );
            unaryStep.inferredType = null;
            return;
        }

        // rezultat ima isti tip
        unaryStep.inferredType = type;
    }


    @Override
    public void visit(Stmt.If ifStmt) {

        // uslov u if-u
        ifStmt.condition.accept(this);
        Ast.VarType condType = ifStmt.condition.inferredType;

        if (condType == null || condType.isArray() || condType.getBaseType() != Ast.BaseType.XOBOOL) {

            reportError(
                    SemanticError.Type.INVALID_CONDITION,
                    ifStmt,
                    null,
                    "if condition must be boolean"
            );
        }

        // then blok — novi scope
        ifStmt.thenBlock.accept(this);


        // poziv njihovih visit metoda
        // else if
        for (Stmt.If.ElseIf elseIf : ifStmt.elseIfs) {
            elseIf.accept(this);
        }

        // else
        if (ifStmt.elseBranch != null) {
            ifStmt.elseBranch.accept(this);
        }
    }

    @Override
    public void visit(Stmt.If.ElseIf elseIfStmt) {

        // uslov
        elseIfStmt.condition.accept(this);
        Ast.VarType condType = elseIfStmt.condition.inferredType;

        if (condType == null || condType.isArray() || condType.getBaseType() != Ast.BaseType.XOBOOL) {

            reportError(
                    SemanticError.Type.INVALID_CONDITION,
                    elseIfStmt,
                    null,
                    "else-if condition must be boolean"
            );
        }

        // blok
        elseIfStmt.block.accept(this);
    }

    @Override
    public void visit(Stmt.If.Else elseStmt) {
        // samo blok, bez uslova
        elseStmt.block.accept(this);
    }


    @Override
    public void visit(Stmt.Timeless timeless) {
        // uslov
        timeless.condition.accept(this);
        Ast.VarType condType = timeless.condition.inferredType;

        if (condType == null || condType.isArray() || condType.getBaseType() != Ast.BaseType.XOBOOL) {
            reportError(
                    SemanticError.Type.INVALID_CONDITION,
                    timeless,
                    null,
                    "timeless condition must be boolean"
            );
        }

        // telo petlje
        timeless.body.accept(this);
    }


    @Override
    public void visit(Stmt.RepeatAfterMe loop) {

        // for ima svoj scope
        enterScope();

        // deklaracija/inicijalizacija
        if (loop.init != null) {
            loop.init.accept(this);
        }

        // uslov
        if (loop.cond != null) {
            loop.cond.accept(this);
            Ast.VarType condType = loop.cond.inferredType;

            if (condType == null || condType.isArray() || condType.getBaseType() != Ast.BaseType.XOBOOL) {
                reportError(
                        SemanticError.Type.INVALID_CONDITION,
                        loop,
                        null,
                        "repeatAfterMe condition must be boolean"
                );
            }
        }

        // step
        if (loop.step != null) {
            loop.step.accept(this);
        }

        // body tj block
        loop.body.accept(this);

        // ===== izlaz iz for-scope-a =====
        exitScope();
    }


    @Override
    public void visit(Stmt.RepeatAfterMe.InitDecl init) {

        String name = init.name.lexeme;

        // duplikat u for scope-u
        if (currentScope.containsLocal(name)) {
            reportError(
                    SemanticError.Type.DUPLICATE_SYMBOL,
                    init,
                    init.name,
                    "variable '" + name + "'"
            );
            return;
        }

        // registracija promenljive
        VarSymbol symbol = new VarSymbol(name, init.type);
        currentScope.define(symbol);

        // inicijalizacija (ako postoji)
        if (init.init != null) {
            init.init.accept(this);
            Ast.VarType exprType = init.init.inferredType;

            if (exprType != null && !exprType.equals(init.type)) {
                reportError(
                        SemanticError.Type.INVALID_ASSIGNMENT,
                        init,
                        init.name,
                        "cannot assign " + exprType + " to " + init.type
                );
            }
        }
    }


    @Override
    public void visit(Stmt.RepeatAfterMe.InitAssign init) {

        // leva strana
        Expr.LValueRef ref = new Expr.LValueRef(init.target);
        ref.accept(this);
        Ast.VarType leftType = ref.inferredType;

        // desna strana
        init.value.accept(this);
        Ast.VarType rightType = init.value.inferredType;

        if (leftType != null && rightType != null && !leftType.equals(rightType)) {
            reportError(
                    SemanticError.Type.INVALID_ASSIGNMENT,
                    init,
                    init.op,
                    "cannot assign " + rightType + " to " + leftType
            );
        }
    }


    @Override
    public void visit(Stmt.RepeatAfterMe.StepAssign step) {

        Expr.LValueRef ref = new Expr.LValueRef(step.target);
        ref.accept(this);
        Ast.VarType leftType = ref.inferredType;

        step.value.accept(this);
        Ast.VarType rightType = step.value.inferredType;

        if (leftType != null && rightType != null && !leftType.equals(rightType)) {
            reportError(
                    SemanticError.Type.INVALID_ASSIGNMENT,
                    step,
                    step.op,
                    "cannot assign " + rightType + " to " + leftType
            );
        }
    }


    @Override
    public void visit(Stmt.RepeatAfterMe.StepUnary stepUnary) {
        stepUnary.step.accept(this);
    }


    @Override
    public void visit(Stmt.Return ret) {

        // sigurnosna provera (return van funkcije)
        if (currentFunctionReturnType == null) {
            return;
        }

        // void fja
        if (currentFunctionReturnType.isVoid) {

            if (ret.expr != null) {
                reportError(
                        SemanticError.Type.INVALID_RETURN_TYPE,
                        ret,
                        ret.ex,
                        "void function cannot return a value"
                );
            }

            return;
        }


        if (ret.expr == null) {
            reportError(
                    SemanticError.Type.INVALID_RETURN_TYPE,
                    ret,
                    ret.ex,
                    "non-void function must return a value"
            );
            return;
        }

        // analiziramo izraz koji vraca
        ret.expr.accept(this);
        Ast.VarType exprType = ret.expr.inferredType;

        Ast.VarType expectedType = currentFunctionReturnType.type;

        // provera tipova (dal se povratni tip slaze sa onimu deklaraciji)
        if (exprType != null && !exprType.equals(expectedType)) {
            reportError(
                    SemanticError.Type.INVALID_RETURN_TYPE,
                    ret,
                    ret.ex,
                    "cannot return " + exprType + " from function returning " + expectedType
            );
        }
    }


    @Override
    public void visit(Stmt.CallStmt callStmt) {
        callStmt.call.accept(this);
    }

    @Override
    public void visit(Expr.Call call) {
        String name = call.name.lexeme;

        // trazimo simbol
        Symbol symbol = currentScope.resolve(name);

        // ne postoji
        if (symbol == null) {
            reportError(
                    SemanticError.Type.UNDECLARED_SYMBOL,
                    call,
                    call.name,
                    "function '" + name + "'"
            );
            call.inferredType = null;
            return;
        }
        // simbol nije fja
        if (!(symbol instanceof FuncSymbol func)) {
            reportError(
                    SemanticError.Type.INVALID_FUNCTION_USAGE,
                    call,
                    call.name,
                    "'" + name + "' is not a function"
            );
            call.inferredType = null;
            return;
        }

        // proveravamo broj argumenata
        if (call.args.size() != func.getParams().size()) {
            reportError(
                    SemanticError.Type.INVALID_ARGUMENT_COUNT,
                    call,
                    call.name,
                    "expected " + func.getParams().size() +
                            " arguments but got " + call.args.size()
            );
        }

        // proveravamo tipove argumenata
        int n = Math.min(call.args.size(), func.getParams().size());
        for (int i = 0; i < n; i++) {

            Expr arg = call.args.get(i);
            Ast.Param param = func.getParams().get(i);

            arg.accept(this);

            Ast.VarType argType = arg.inferredType;
            Ast.VarType paramType = param.type;


            if (argType != null && !argType.equals(paramType)) {
                reportError(
                        SemanticError.Type.INVALID_ARGUMENT_TYPE,
                        call,
                        call.name,
                        "argument " + (i + 1) +
                                " expected " + paramType +
                                " but got " + argType
                );
            }
        }

        // povratni tip
        if (func.getRetType().isVoid) {
            call.inferredType = null; // nema vrednost
        } else {
            call.inferredType = func.getRetType().type;
        }
    }


    @Override
    public void visit(Expr.Cast cast) {

        // prvo obradimo izraz koji se kastuje
        cast.expr.accept(this);

        // izvucemo njegov tip
        Ast.VarType exprType = cast.expr.inferredType;

        if (exprType == null) {
            cast.inferredType = null;
            return;
        }

        // ne sme kastovanje nizova
        if (exprType.isArray()) {
            reportError(
                    SemanticError.Type.INVALID_CAST,
                    cast,
                    null,
                    "cannot cast array type"
            );
            cast.inferredType = null;
            return;
        }

        Ast.BaseType from = exprType.baseType;
        Ast.BaseType to = cast.targetType;


        // xoNum -> xoDouble (uvek dozvoljeno)
        if (from == Ast.BaseType.XONUM && to == Ast.BaseType.XODOUBLE) {
            cast.inferredType = new Ast.VarType(to);
            return;
        }

        // xoDouble -> xoNum (samo ako je .0)
        if (from == Ast.BaseType.XODOUBLE && to == Ast.BaseType.XONUM) {

            // dozvoljeno samo ako je literal tipa Real
            if (cast.expr instanceof Expr.RealLit real) {

                double value = real.value;
                if (value == Math.floor(value)) {
                    cast.inferredType = new Ast.VarType(to);
                    return;
                }
            }
            reportError(
                    SemanticError.Type.INVALID_CAST,
                    cast,
                    null,
                    "cannot cast non-integer real value to xoNum"
            );
            cast.inferredType = null;
            return;
        }

        // inace greska
        reportError(
                SemanticError.Type.INVALID_CAST,
                cast,
                null,
                "cannot cast from " + from + " to " + to
        );
        cast.inferredType = null;


    }



    @Override
    public void visit(Ast.BlockMain blockMain) {
        enterScope();

        for (Stmt stmt : blockMain.stmts) {
            if (stmt != null) stmt.accept(this);
        }

        exitScope();
    }



    @Override
    public void visit(Ast.LValue lValue) {
        if (lValue.indices != null) {
            for (Expr idx : lValue.indices) {
                if (idx != null) idx.accept(this);
            }
        }
    }



    @Override
    public void visit(Expr.Grouping grouping) {
        grouping.inner.accept(this);
        grouping.inferredType = grouping.inner.inferredType;
    }



    @Override
    public void visit(Expr.ArrayLit arrayLit) {
    // prazan niz -> ne mozemo pouzdano da odredimo tip
        if (arrayLit.elements == null || arrayLit.elements.isEmpty()) {
            arrayLit.inferredType = null;
            return;
        }

        // obradimo prvi element kao referentni tip
        Expr first = arrayLit.elements.get(0);
        first.accept(this);

        Ast.VarType firstType = first.inferredType;

        if (firstType == null) {
            arrayLit.inferredType = null;
            return;
        }

        // svi ostali elementi moraju imati isti tip
        for (int i = 1; i < arrayLit.elements.size(); i++) {
            Expr e = arrayLit.elements.get(i);
            e.accept(this);

            Ast.VarType t = e.inferredType;

            if (t == null || !t.equals(firstType)) {
                reportError(
                        SemanticError.Type.ARRAY_ELEMENT_TYPE_MISMATCH,
                        arrayLit,
                        null,
                        "array literal elements must have the same type"
                );
                arrayLit.inferredType = null;
                return;
            }
        }

        // tip elemenata niza
        arrayLit.inferredType = new Ast.VarType(firstType);
    }



    @Override
    public void visit(Stmt.Listen listenStmt) {
        Expr.LValueRef ref = new Expr.LValueRef(listenStmt.target);
        // obradjujemo vrednost U KOJU UPISUJEMO
        ref.accept(this);
    }

    @Override
    public void visit(Stmt.Echo echoStmt) {
        // obradjujemo expr koji ispisujemo
        if (echoStmt.expr != null) {
            echoStmt.expr.accept(this);
        }
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
    public void visit(Expr.NumLit numLit) {
        numLit.inferredType = new Ast.VarType(Ast.BaseType.XONUM);
    }

    @Override
    public void visit(Expr.RealLit realLit) {
        realLit.inferredType = new Ast.VarType(Ast.BaseType.XODOUBLE);
    }

    @Override
    public void visit(Expr.StringLit stringLit) {
        stringLit.inferredType = new Ast.VarType(Ast.BaseType.XOSTRING);
    }

    @Override
    public void visit(Expr.CharLit charLit) {
        charLit.inferredType = new Ast.VarType(Ast.BaseType.XOCHAR);
    }

    @Override
    public void visit(Expr.BoolLit boolLit) {
        boolLit.inferredType = new Ast.VarType(Ast.BaseType.XOBOOL);
    }

    public List<SemanticError> getErrors() {
        return errors;
    }
}