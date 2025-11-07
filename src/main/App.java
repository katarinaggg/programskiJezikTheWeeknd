package main;

import lexer.Lexer;
import lexer.token.Token;
import lexer.token.TokenFormatter;
import parser.Node;
import parser.Parser;

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
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.scanTokens();

            System.out.println(TokenFormatter.formatList(tokens));


            // 2. Pokrećemo parser
            Parser parser = new Parser(tokens);
            Node program = parser.parseProgram();

            System.out.println("\n=== AST ===");
            program.print("");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
