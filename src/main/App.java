package main;

import lexer.Lexer;
import lexer.token.Token;
import lexer.token.TokenFormatter;

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

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
