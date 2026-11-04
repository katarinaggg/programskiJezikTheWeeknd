# The Weeknd — a custom programming language and compiler front-end in Java

**The Weeknd** is a small, statically typed, imperative programming language designed and implemented from scratch as a team project for the university course *Programski prevodioci* (Programming Language Translators / Compilers).

This repository is not an application built *with* a language. It **is** the language: its grammar, plus a hand-written compiler pipeline in plain Java (no parser generators, no external libraries) that takes a source program through

**lexical analysis → syntax analysis (AST) → semantic analysis (type checking, scopes) → intermediate code generation**

and writes a stack-based intermediate representation to a file.

The keywords are themed after The Weeknd (`XOTWOD`, `CALLOUT`, `ECHO`, `EXODUS`, `tryMe`, `orNah`, `repeatAfterMe`, `timeless`, `rise`, `fade`, and others).

---

## Table of Contents

- [Language at a Glance](#language-at-a-glance)
- [Features](#features)
- [Language Syntax and Examples](#language-syntax-and-examples)
- [Architecture](#architecture)
- [How It Works](#how-it-works)
- [Intermediate Code](#intermediate-code)
- [Error Handling](#error-handling)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [How to Run](#how-to-run)
- [Example Programs and Testing](#example-programs-and-testing)
- [Known Limitations](#known-limitations)
- [Technologies Used](#technologies-used)
- [Team](#team)

---

## Language at a Glance

The forms below are schematic and follow the grammar in `Gramatika.txt`. Real programs are shown in [Language Syntax and Examples](#language-syntax-and-examples).

| Concept | The Weeknd | Typical equivalent |
|---|---|---|
| Program entry point (main) | `XOTWOD { ... EXODUS; }` | `main() { ... }` |
| Variable / function declaration | `CALLOUT` | declaration keyword |
| Assignment | `~>` | `=` |
| Return | `EXODUS` / `EXODUS expr;` | `return` |
| Output / Input | `ECHO(expr)` / `LISTEN(var)` | `print` / `read` |
| If / else-if / else | `tryMe (...) {}` `next (...) {}` `orNah {}` | `if` / `else if` / `else` |
| For loop | `repeatAfterMe(init; cond; step) {}` | `for` |
| While loop | `timeless(cond) {}` | `while` |
| Increment / Decrement | `rise lvalue` / `fade lvalue` | `++x` / `--x` |
| Logical operators | `xoAnd`, `xoOr`, `xoNot` | `&&`, `\|\|`, `!` |
| Equality / inequality | `=`, `!=` | `==`, `!=` |
| Boolean literals | `TRUE`, `FALSE` | `true`, `false` |

**Types:** `xoNum` (integer), `xoDouble` (real), `xoChar`, `xoBool`, `xoString`, `xoVoid` (function return type only), and arrays of any dimension via `xoArray` (for example `xoArray xoArray xoNum` is a 2D integer array).

---

## Features

### Lexer (`src/lexer`)
- Hand-written character scanner (`ScannerCore`) that tracks line and column for every token.
- Keyword recognition with a keyword → `TokenType` map.
- Integer and real literals, char literals, string literals, `TRUE` / `FALSE`.
- Escape sequences: `\n`, `\t`, `\\`, `\"` (strings) and `\'` (chars).
- Single-line `//` comments and **nested** multi-line `/* */` comments, with nesting depth tracked (see `komentari_xoVoidfja.txt`).
- Lexical errors with position info: unexpected characters, unterminated strings/chars/comments, invalid escape sequences, and a letter directly after a number.
- Prints a formatted token table (`TokenFormatter`).

### Parser and AST (`src/parser`)
- Hand-written **recursive-descent parser** that follows the EBNF grammar in [`src/resources/gramatika/Gramatika.txt`](src/resources/gramatika/Gramatika.txt).
- Operator precedence encoded in the grammar levels: `xoOr` < `xoAnd` < `xoNot` < relational < `+ -` < `* / %` < unary (cast, unary `+ -`, `rise` / `fade`) < primary.
- Builds a typed **Abstract Syntax Tree** (`Ast`, `Stmt`, `Expr` node classes), which is printed as an indented tree with node IDs and inferred types.
- Functions may be declared both **before and after** the main block.
- Function calls are supported both as expressions and as standalone statements.
- Multi-dimensional array literals (nested `[ ... ]`) and multi-index access such as `matrix[i][j]` (see `matrica_repeatPetlja.txt`).

### Semantic analysis (`src/semantic`)
- **Two-phase analysis:** all functions are registered first (so a function can be called before its declaration), then the AST is traversed with the Visitor pattern.
- **Nested scopes** (`Scope` chain with parent links) for functions, blocks, loop headers and the main block.
- **Symbol table** with `VarSymbol` and `FuncSymbol`.
- **Type inference and type checking** for every expression (the result is stored in `Expr.inferredType`):
  - arithmetic requires both operands to be `xoNum` or both `xoDouble` (no implicit conversion);
  - relational operators require numeric operands of the same type and produce `xoBool`;
  - logical operators require `xoBool`;
  - conditions in `tryMe`, `next`, `timeless` and `repeatAfterMe` must be `xoBool`;
  - array index must be `xoNum`, only arrays can be indexed, array literal elements must share one type;
  - assignment and initialization type compatibility, including array dimensions.
- **Function checks:** number and types of arguments, return type compatibility, `xoVoid` functions returning a value, calling a variable as a function.
- **Program checks:** missing main block, multiple main blocks, duplicate symbols in one scope, undeclared symbols.
- **Explicit casts:** `(xoDouble)` from `xoNum` is always allowed; `(xoNum)` from `xoDouble` is allowed only for a real literal with a zero fractional part (such as `12.0`) or a variable initialized with one.
- Collects **all** semantic errors (it does not stop at the first one) and prints them in a structured format.

### Intermediate code generation (`src/intermediate`)
- Translates the AST into a **stack-based intermediate code** (push/pop, arithmetic, comparison, jumps).
- **Unique label generation** (`LabelManager`) for `if` / `else-if` / loops / function returns.
- **Two-pass label resolution** (`InstructionEmitter`): symbolic labels are emitted first and then replaced with concrete instruction indices before the code is written to `output.txt`.
- Function calling convention: arguments are pushed, then the return address; the callee pops the return address into `IP` and the parameters in reverse order, and returns with `jmp IP`.

---

## Language Syntax and Examples

All examples below are copied **verbatim** from the example programs in [`src/resources/primeriKodova`](src/resources/primeriKodova). The comments inside them are in Serbian, as in the original files. Each example is a complete file that compiles without errors.

### Functions, variables and output — `funkc_saberi_ispisi.txt` (full file)
This file shows a function that returns a value (`xoNum`) and an `xoVoid` function, both declared before the main block. It also shows variable declarations with initialization (`CALLOUT ... ~>`), a function call used as an expression (`saberi(x, y)`), a call used as a standalone statement (`ispisi(zbir);`), output with `ECHO`, and `EXODUS` as both a valued and an empty return.
```
CALLOUT xoNum saberi(xoNum a, xoNum b) {
    CALLOUT xoNum rezultat ~> a + b;
    EXODUS rezultat;
}

CALLOUT xoVoid ispisi(xoNum n) {
    ECHO(n);
    EXODUS;
}

XOTWOD {
    CALLOUT xoNum x ~> 10;
    CALLOUT xoNum y ~> 25;

    CALLOUT xoNum zbir ~> saberi(x, y);
    ispisi(zbir);

    EXODUS;
}
```

### Conditionals — `tryMe.txt` (full file)
This file shows the full conditional chain: `tryMe` (if), `next` (else if) and `orNah` (else). It also shows relational operators. Note that `=` is the equality operator, because assignment uses `~>`.
```
XOTWOD {
    CALLOUT xoNum broj ~> 7;

    tryMe(broj > 10) {
        ECHO("Broj je veci od 10");
    }
    next(broj = 7) {
        ECHO("Broj je tacno 7");
    }
    orNah {
        ECHO("Broj nije ni veci od 10 ni 7");
    }
    EXODUS;
}
```

### Two-dimensional array and nested loops — `matrica_repeatPetlja.txt` (full file)
This file declares a 2D array type (`xoArray xoArray xoNum`) initialized with a nested array literal, then walks it with two nested `repeatAfterMe` (for) loops. Each loop declares its own counter in the loop header and steps it with `rise`. Elements are read with multiple indices (`matrix[i][j]`).
```
XOTWOD {
    CALLOUT xoArray xoArray xoNum matrix ~>
    [
        [1, 2, 3],
        [4, 5, 6],
        [7, 8, 9]
    ];

    ECHO("Elementi matrice:");

    repeatAfterMe(CALLOUT xoNum i ~> 0; i < 3; rise i) {
        repeatAfterMe(CALLOUT xoNum j ~> 0; j < 3; rise j) {
            ECHO(matrix[i][j]);
        }
    }

    EXODUS;
}
```

### Comments, `for` and `while` loops — `komentari_xoVoidfja.txt` (full file)
This file shows a **nested** multi-line comment (a `/* ... */` inside another `/* ... */`), single-line `//` comments, a `repeatAfterMe` (for) loop, a `timeless` (while) loop, and a call to an `xoVoid` function with a string argument.
```
/* Glavni program sa komentarima
   i testiranjem void funkcije.
   /* Ugnježdeni komentar takođe radi! */
*/

CALLOUT xoVoid pozdrav(xoString ime) {
    ECHO("Pozdrav, ");
    ECHO(ime);
    EXODUS;
}

XOTWOD {

    // Inicijalizacija sume
    CALLOUT xoNum suma ~> 0;

    // For-petlja: sabira brojeve 0 do 4
    repeatAfterMe(CALLOUT xoNum i ~> 0; i < 5; rise i) {
        suma ~> suma + i;
    }

    /* Ako je suma manja od nule (nije, ali čisto testiramo)
       izvršava se ovaj blok.
    */
    timeless(suma < 0) {
        suma ~> suma - 2;
    }

    // Poziv void funkcije
    pozdrav("XO Language User");

    // Ispis rezultata
    ECHO("Krajnja vrednost sume je:");
    ECHO(suma);

    EXODUS;
}
```

### Explicit casting — `kastovanje.txt` (full file)
This file shows the casting rules enforced by the semantic analyzer. `(xoDouble)` applied to an `xoNum` is always allowed. `(xoNum)` applied to an `xoDouble` is accepted only because `z` was initialized with a real literal whose fractional part is zero (`12.0`). A cast can also be applied to the result of a function call.
```
CALLOUT xoNum saberi(xoNum a, xoNum b) {
    EXODUS a + b;
}

XOTWOD {
    CALLOUT xoNum x ~> 7;

    // eksplicitno kastovanje iz xoNum u xoDouble (uvek dozvoljeno)
    CALLOUT xoDouble y ~> (xoDouble) x;

    // realan broj, ali sa .0 (decimalni deo je 0)
    CALLOUT xoDouble z ~> 12.0;

    // eksplicitno kastovanje iz xoDouble u xoNum
    // DOZVOLJENO jer je decimalni deo = 0
    CALLOUT xoNum w ~> (xoNum) z;

    // koriscenje kastovanih vrednosti u izrazu
     CALLOUT xoDouble rezultat ~> (xoDouble) saberi(x, w);

    ECHO("Rezultat je:");
    ECHO(rezultat);

    EXODUS;
}
```

### Strings, chars, booleans and logical operators — `string_char_bool_logOp.txt` (full file)
This file shows a function that returns `xoBool` from both branches of a `tryMe` / `orNah`, the boolean literals `TRUE` / `FALSE`, an `xoChar` literal (`'!'`), `xoString` variables and arguments, and the logical operators `xoNot`, `xoOr` and `xoAnd`.
```
/* ===========================================
   Test za string, char, bool i logičke operatore
   =========================================== */


CALLOUT xoBool isti(xoNum x, xoNum y) {
    tryMe (x = y) {
        EXODUS TRUE;
    } orNah {
        EXODUS FALSE;
    }
}

// void funkcija za prikaz pozdrava
CALLOUT xoVoid pozdrav(xoString ime) {
    ECHO("Zdravo, ");
    ECHO(ime);
    CALLOUT xoChar znak ~> '!';
    ECHO(znak);
    ECHO(" Dobrodošao u XO jezik.");
    EXODUS;
}

XOTWOD {

    CALLOUT xoNum x ~> 10;
    CALLOUT xoNum y ~> 10;
    CALLOUT xoBool rez ~> isti(x, y);

    // testiranje logičkih izraza
    CALLOUT xoBool test1 ~> xoNot rez;               // negacija
    CALLOUT xoBool test2 ~> (rez xoOr test1);        // OR
    CALLOUT xoBool test3 ~> (rez xoAnd test2);       // AND

    // poziv void funkcije
    CALLOUT xoString k ~> "XO korisniče";
    pozdrav(k);

    ECHO("Rezultat provere brojeva:");
    ECHO(rez);

    ECHO("Negacija (xoNot):");
    ECHO(test1);

    ECHO("OR (xoOr) rezultat:");
    ECHO(test2);

    ECHO("AND (xoAnd) rezultat:");
    ECHO(test3);

    EXODUS;
}
```

### Examples of rejected programs
The `lexGreska*`, `sintaksnaGreska*` and `semantickaGreska*` files are intentionally invalid programs. They are listed in [Example Programs and Testing](#example-programs-and-testing) together with the errors they trigger, rather than shown here as syntax examples.

### Program structure rules
- Every program must contain **exactly one** `XOTWOD` block, which must end with `EXODUS;` (without a return value).
- Functions are declared with `CALLOUT <return type> name(params) { ... }` and may appear before or after `XOTWOD`.
- Variable declaration: `CALLOUT <type> name;` or `CALLOUT <type> name ~> expr;`.
- Every statement ends with `;`, and blocks for `tryMe`, `next`, `orNah` and loops always require `{ }`.

The full grammar (EBNF) is in [`src/resources/gramatika/Gramatika.txt`](src/resources/gramatika/Gramatika.txt).

---

## Architecture

```
            source file (.txt)
                   │
                   ▼
   ┌──────────────────────────────┐
   │ Lexer  (lexer.Lexer,         │  characters → List<Token>
   │         lexer.ScannerCore)   │  + printed token table
   └──────────────┬───────────────┘
                  ▼
   ┌──────────────────────────────┐
   │ Parser (parser.ParserAst)    │  tokens → AST (Ast.Program)
   │ recursive descent            │
   └──────────────┬───────────────┘
                  ▼
   ┌──────────────────────────────┐
   │ SemanticAnalyzer (Visitor)   │  scopes, symbols, types
   │                              │  → annotates Expr.inferredType
   └──────────────┬───────────────┘
                  │ only if there are no semantic errors
                  ▼
   ┌──────────────────────────────┐
   │ AST printed with types       │
   │ CodeGenerator (Visitor)      │  AST → stack-based instructions
   │ InstructionEmitter           │  label → index resolution
   └──────────────┬───────────────┘
                  ▼
              output.txt
```

**Design principles used**
- **Visitor pattern:** `visitor.Visitor` declares a `visit` method for every AST node type. `SemanticAnalyzer` and `CodeGenerator` are two independent visitors over the same tree, so adding a new compiler pass doesn't require changing the node classes.
- **Separation of phases:** each phase has its own package and communicates only through well-defined data (a `List<Token>`, the AST, and the AST annotated with types).
- **Composite tree:** all nodes extend `NodeBase`, which keeps a list of children, a unique ID and a generic `print` method.

---

## How It Works

1. **`main.App`** reads the source file passed as the single command-line argument.
2. **Lexing:** `Lexer.scanTokens()` skips whitespace and comments, recognizes tokens character by character using `ScannerCore` (`peek`, `peekNext`, `advance`, `match`), and appends an `EOF` token. The token table is printed to standard output.
3. **Parsing:** `ParserAst.parse()` applies one method per grammar rule (`parseProgram`, `parseStmt`, `parseExpr`, `parseOr`, `parseAnd`, ... `parsePrimary`), using one or two tokens of lookahead. For example, a cast such as `(xoDouble) x` is told apart from a grouped expression such as `(2 + 3)` by checking whether a type keyword follows `(`. A function call such as `saberi(x, y)` is told apart from a variable reference by checking whether `(` follows the identifier.
4. **Semantic analysis:** `SemanticAnalyzer.analyze()` registers functions, checks the main block, then visits the tree, opens and closes scopes, resolves names, and infers and checks types. All errors are collected and printed.
5. **Code generation:** if there are no semantic errors, the type-annotated AST is printed, and `CodeGenerator` emits instructions. `InstructionEmitter.writeToFile("output.txt")` resolves labels to instruction indices and writes the result.

---

## Intermediate Code

The generator produces a stack-machine-style instruction list (instruction indices start at 0; a label is materialized as a `nop` instruction):

| Group | Instructions |
|---|---|
| Stack / variables | `push <literal \| variable \| address>`, `pop <variable>`, `pop` |
| Arithmetic | `add`, `sub`, `mul`, `div`, `mod`, `neg` |
| Comparison / logic | `lt`, `gt`, `le`, `ge`, `eq`, `ne`, `and`, `or`, `not` |
| Control flow | `jmp <n>`, `jmp_zero <n>`, `jmp IP`, `pop IP`, `nop` |
| I/O | `echo`, `listen <variable>` |
| Arrays | `new_array <n>`, `array_get` |
| Casts | `cast_xodouble`, `cast_xonum` |

For example, the `tryMe` program shown above compiles to:

```
jmp 1
nop
push 7
pop broj
push broj
push 10
gt
jmp_zero 11
push "Broj je veci od 10"
echo
jmp 22
nop
push broj
push 7
eq
jmp_zero 19
push "Broj je tacno 7"
echo
jmp 22
nop
push "Broj nije ni veci od 10 ni 7"
echo
nop
```

> The project generates this intermediate code but does **not** include a virtual machine or interpreter that executes it. Programs are compiled to `output.txt`, not run.

---

## Error Handling

Each phase reports errors in its own way:

| Phase | Behavior | Example output |
|---|---|---|
| Lexical | Throws on the first error, and `App` prints it and exits with code `1` | `Error: LEXER > Unexpected character at 4:12 near '$'` |
| Syntax | Prints the expected construct, the found token and the last read token, then exits with code `1` (no error recovery) | `PARSER ERROR: Expected ';' after variable declaration. Found: CALLOUT 'CALLOUT' at 3:5` |
| Semantic | Collects all errors, prints each one with its type, location and message, and skips code generation | `SEMANTIC ERROR [DUPLICATE_SYMBOL] ... Location : (line 4, col 19) ...` |

Semantic error categories (`semantic.SemanticError.Type`) include `NO_MAIN_FUNCTION`, `MULTIPLE_MAIN_FUNCTIONS`, `UNDECLARED_SYMBOL`, `DUPLICATE_SYMBOL`, `INVALID_CAST`, `INVALID_ASSIGNMENT`, `ARITHMETIC_TYPE_MISMATCH`, `RELATIONAL_OPERAND_NOT_NUMBER`, `LOGICAL_OPERAND_NOT_BOOLEAN`, `INVALID_CONDITION`, `ARRAY_ELEMENT_TYPE_MISMATCH`, `INVALID_INDEXING`, `INVALID_RETURN_TYPE`, `INVALID_ARGUMENT_COUNT`, `INVALID_ARGUMENT_TYPE` and `INVALID_FUNCTION_USAGE`.

---

## Project Structure

```
programskiJezikTheWeeknd/
├── src/
│   ├── main/
│   │   └── App.java                 # entry point, runs the whole pipeline
│   ├── lexer/
│   │   ├── Lexer.java               # tokenizer, keywords, literals, comments
│   │   ├── ScannerCore.java         # character cursor with line/column tracking
│   │   └── token/
│   │       ├── Token.java           # type, lexeme, literal value, position
│   │       ├── TokenType.java       # all token kinds
│   │       └── TokenFormatter.java  # token table printing
│   ├── parser/
│   │   ├── ParserAst.java           # recursive-descent parser
│   │   └── ast/
│   │       ├── NodeBase.java        # base node: children, id, tree printing
│   │       ├── Ast.java             # Program, BlockMain, FuncDecl, Block, VarType, ...
│   │       ├── Stmt.java            # statements (VarDecl, Assign, If, loops, ...)
│   │       └── Expr.java            # expressions (literals, Binary, Unary, Call, Cast, ...)
│   ├── visitor/
│   │   └── Visitor.java             # visitor interface over all AST nodes
│   ├── semantic/
│   │   ├── SemanticAnalyzer.java    # scopes, symbol resolution, type checking
│   │   ├── Scope.java               # nested scope with parent link
│   │   ├── Symbol.java / VarSymbol.java / FuncSymbol.java
│   │   └── SemanticError.java       # error categories and formatting
│   ├── intermediate/
│   │   ├── CodeGenerator.java       # AST → stack-based intermediate code
│   │   ├── InstructionEmitter.java  # instruction buffer, label resolution, file output
│   │   └── LabelManager.java        # unique label names
│   └── resources/
│       ├── gramatika/Gramatika.txt  # EBNF grammar of the language
│       └── primeriKodova/           # example programs (valid and erroneous)
└── .gitignore
```

---

## Getting Started

### Requirements
- **JDK 16 or newer.** The code uses switch expressions and pattern matching for `instanceof`. The original IntelliJ project was configured for JDK 22 (Amazon Corretto), and the build was also verified with JDK 21.
- No external libraries and no build tool (Maven or Gradle). Only the standard Java library is used.

### Clone
```bash
git clone https://github.com/katarinaggg/programskiJezikTheWeeknd.git
cd programskiJezikTheWeeknd
```

### Compile
From the project root:

**Bash / Git Bash / macOS / Linux**
```bash
javac -d out $(find src -name "*.java")
```

**Windows PowerShell**
```powershell
javac -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
```

`out/` is already listed in `.gitignore`.

**IntelliJ IDEA:** open the folder as a project, mark `src` as the sources root, and run `main.App` with the path to a source file as the program argument.

---

## How to Run

```bash
java -cp out main.App <path-to-source-file>
```

Example:
```bash
java -cp out main.App src/resources/primeriKodova/funkc_saberi_ispisi.txt
```

The program:
1. prints the **token table**;
2. prints the **semantic errors**, if any, and stops;
3. otherwise, prints the **AST with inferred types** and writes the intermediate code to **`output.txt` in the current working directory**.

---

## Example Programs and Testing

The project has no automated unit tests. Testing is done with example programs in [`src/resources/primeriKodova`](src/resources/primeriKodova), each of which targets a specific feature or error class:

| File | Purpose | Result |
|---|---|---|
| `funkc_saberi_ispisi.txt` | functions, calls, `xoVoid` | compiles |
| `kastovanje.txt` | explicit casts | compiles |
| `komentari_xoVoidfja.txt` | nested comments, `repeatAfterMe`, `timeless` | compiles |
| `matrica_repeatPetlja.txt` | 2D array literal, nested loops, indexing | compiles |
| `proba`, `redosledOperacija.txt` | operator precedence, `rise` inside expressions | compiles |
| `string_char_bool_logOp.txt` | strings, chars, booleans, logical operators | compiles |
| `tryMe.txt` | `tryMe` / `next` / `orNah` | compiles |
| `lexGreska1-3.txt` | invalid character, invalid number suffix, unterminated string | lexical error |
| `sintaksnaGreska1-3.txt` | missing `;`, missing `{`, missing `]` | syntax error |
| `semantickaGreska1-3` | duplicates, undeclared names, bad conditions, type mismatches, missing/multiple main, argument count, calling a variable as a function | semantic errors |

The results in the table were checked by compiling the project and running each example.

---

## Known Limitations

This is an educational compiler front-end. These parts are not finished or are simplified:

- **No execution:** there is no VM or interpreter for the generated intermediate code.
- **Array element writes:** the grammar allows an indexed lvalue (`IDENT { "[" expr "]" }`) as the target of assignment, `LISTEN` and `rise` / `fade`, and semantic analysis accepts it. However, `CodeGenerator` ignores the indices in these cases and emits `pop` / `listen` on the whole variable name. Only array element **reads** (`array_get`) are generated correctly.
- **Functions in generated code:** variables and the return address (`IP`) are global names, and there are no stack frames, so recursive or nested calls would overwrite them. A standalone call to an `xoVoid` function still emits a `pop` for a return value that does not exist.
- **Semantic checks not yet covered:** the analyzer does not report an error for using the result of an `xoVoid` call as a value, for a non-void function that has no `EXODUS <expr>`, or for an empty array literal.
- **Equality on non-numeric types:** `=` and `!=` are handled like the other relational operators, so they accept only numeric operands. Comparing `xoBool`, `xoChar` or `xoString` values is rejected.
- **Error handling:** lexical and syntax errors stop at the first error, and there is no parser error recovery. A program with semantic errors still exits with code `0`.

---

## Technologies Used

- **Java** (standard library only: `java.util`, `java.nio.file`)
- Hand-written **lexer** and **recursive-descent parser** (no ANTLR, JavaCC, JFlex or other generators)
- **EBNF** grammar specification
- **Visitor** design pattern for compiler passes
- **IntelliJ IDEA** (original development environment)
- **Git and GitHub**, with a feature-branch and pull-request workflow

---

## Team

Developed as a team project for the *Programski prevodioci* course at the Faculty of Computer Science (RAF), Belgrade, by:

- **Katarina Gordić** 
- **Anja Stanković**
