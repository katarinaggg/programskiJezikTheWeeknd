package main;

import intermediate.CodeGenerator;
import intermediate.InstructionEmitter;
import intermediate.LabelManager;
import lexer.Lexer;
import lexer.token.Token;
import lexer.token.TokenFormatter;
import parser.ParserAst;
import parser.ast.Ast;
import parser.ast.NodeBase;
import semantic.SemanticAnalyzer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class App {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java main.Application <source-file>");
            System.exit(64);
        }

        try {
            String source = Files.readString(Path.of(args[0]));

            // LEKSICKA ANALIZA
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.scanTokens();
            System.out.println(TokenFormatter.formatList(tokens));

            // SINTAKSNA ANALIZA
            ParserAst parserAst = new ParserAst(tokens);
            Ast.Program program = parserAst.parse();

            // SEMANTICKA ANALIZA
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
            semanticAnalyzer.analyze(program);

            // dodati tipovi na stablo (ako je prosla sem. analiza)
            if(semanticAnalyzer.getErrors().isEmpty()) {
                System.out.println("\n=== AST ===");
                program.print("");



                // GENERISANJE MEDJUKODA
                CodeGenerator codeGenerator = new CodeGenerator();
                //codeGenerator.generate(program);
                //codeGenerator.writeToFile("output.txt");

            }


        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
