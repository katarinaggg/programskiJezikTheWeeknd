package main;

import lexer.Lexer;
import lexer.token.Token;
import lexer.token.TokenFormatter;
import parser.ParserAst;
import parser.ast.NodeBase;
import parser.parserAlt.Node;
import parser.parserAlt.Parser;

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

            // alternativni parser (radi samo sa Node)
            // iz paketa parserAlt
            /*
            Parser parser = new Parser(tokens);
            Node program1 = parser.parseProgram();
            System.out.println("\n=== AST ===");
            program1.print("");
            */

            ParserAst parserAst = new ParserAst(tokens);
            NodeBase program2 = parserAst.parse();
            System.out.println("\n=== AST ===");
            program2.print("");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
