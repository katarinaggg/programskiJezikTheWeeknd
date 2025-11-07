package lexer;

import lexer.token.Token;
import lexer.token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Lexer {
    private final ScannerCore sc;
    private final String source;
    private final List<Token> tokeni = new ArrayList<>();

    // mapa kljucnih reci
    // kljuc = tacan naziv kljucne reci, vrednost = njen tip
    private static final Map<String, TokenType> keywords = Map.ofEntries(
        Map.entry("xoNum", TokenType.XONUM),
        Map.entry("xoDouble", TokenType.XODOUBLE),
        Map.entry("xoChar", TokenType.XOCHAR),
        Map.entry("xoBool", TokenType.XOBOOL),
        Map.entry("xoString", TokenType.XOSTRING),
        Map.entry("xoVoid", TokenType.XOVOID),
        Map.entry("xoArray", TokenType.XOARRAY),
        Map.entry("TRUE", TokenType.BOOL_LIT),
        Map.entry("FALSE", TokenType.BOOL_LIT),
        Map.entry("XOTWOD", TokenType.XOTWOD),
        Map.entry("EXODUS", TokenType.EXODUS),
        Map.entry("CALLOUT", TokenType.CALLOUT),
        Map.entry("LISTEN", TokenType.LISTEN),
        Map.entry("ECHO", TokenType.ECHO),
        Map.entry("rise", TokenType.RISE),
        Map.entry("fade", TokenType.FADE),
        Map.entry("xoAnd", TokenType.XOAND),
        Map.entry("xoOr", TokenType.XOOR),
        Map.entry("xoNot", TokenType.XONOT),
        Map.entry("tryMe", TokenType.TRYME),
        Map.entry("next", TokenType.NEXT),
        Map.entry("orNah", TokenType.ORNAH),
        Map.entry("repeatAfterMe", TokenType.REPEATAFTERME),
        Map.entry("timeless", TokenType.TIMELESS)
    );

    public Lexer(String source) {
        this. sc = new ScannerCore(source);
        this.source = source;
    }

    // preskakanje belina i komentra
    private void skipWhitespaceAndComments() {
        boolean skipping = true;
        while (skipping && !sc.isAtEnd()) {
            skipping = false;

            // preskaci beline
            while (Character.isWhitespace(sc.peek())) {
                sc.advance();
                skipping = true;
            }

            // jednolinijski komentari
            if (sc.peek() == '/' && sc.peekNext() == '/') {
                while (sc.peek() != '\n' && !sc.isAtEnd()) {
                    sc.advance();
                }
                skipping = true;
            }

            // viselinijski i ugnjezdeni komentari
            if (sc.peek() == '/' && sc.peekNext() == '*') {
                sc.advance(); // '/'
                sc.advance(); // '*'
                int comment = 1; // nivo ugnjezdenosti

                while (comment > 0 && !sc.isAtEnd()) {
                    if (sc.peek() == '/' && sc.peekNext() == '*') {
                        comment++; // novi ugnjezdeni komentar
                        sc.advance();
                        sc.advance();
                    } else if (sc.peek() == '*' && sc.peekNext() == '/') {
                        comment--; // zatvaranje jednog nivoa
                        sc.advance();
                        sc.advance();
                    } else {
                        sc.advance();
                    }
                }

                if (comment != 0) {
                    throw error("Unterminated comment");
                }

                skipping = true;
            }
        }
    }


    // skenira tokene i stavlja ih u listu
    public List<Token> scanTokens() {
        while (!sc.isAtEnd()) {
            skipWhitespaceAndComments();
            sc.beginToken();
            if (sc.isAtEnd()) break; // da ne pokrene scanToken posle EOF
            scanToken();
        }
        tokeni.add(new Token(TokenType.EOF, "\0", null, sc.getLine(), sc.getCol(), sc.getCol()));
        return tokeni;
    }

    private void scanToken() {
        char c = sc.advance();

        switch (c){
            case '(' -> add(TokenType.LPAREN);
            case ')' -> add(TokenType.RPAREN);
            case '[' -> add(TokenType.LBRACKET);
            case ']' -> add(TokenType.RBRACKET);
            case '{' -> add(TokenType.LBRACE);
            case '}' -> add(TokenType.RBRACE);
            case ',' -> add(TokenType.COMMA);
            case ';' -> add(TokenType.SEMICOLON);
            case '+' -> add(TokenType.ADD);
            case '-' -> add(TokenType.SUBTRACT);
            case '*' -> add(TokenType.MULTIPLY);
            case '/' -> add(TokenType.DIVIDE);
            case '%' -> add(TokenType.PERCENT);
            case '<' -> add(sc.match('=') ? TokenType.LE : TokenType.LT);
            case '>' -> add(sc.match('=') ? TokenType.GE : TokenType.GT);
            case '=' -> add(TokenType.EQ);
            case '\'' -> charLit();
            case '"' -> stringLit();
            case '~' -> {
                if(sc.match('>'))
                    add(TokenType.ASSIGN);
                else throw error("Unexpected '~'");
            }
            case '!' -> {
                if (sc.match('='))
                    add(TokenType.NEQ);
                else throw error("Unexpected '!'");
            }
            case '\n',' ', '\r', '\t' -> {}
            default -> {
                if(Character.isDigit(c)) number();
                else if(isIdentStart(c)) identifier();
                else throw error("Unexpected character");
            }
        }
    }

    // pravi literal broj
    private void number() {

        boolean d = false;

        // sve dok je trenutni karakter cifra krecemo se dalje
        while (Character.isDigit(sc.peek()))
            sc.advance();

        // ako imamo tacku i posle nje cifre -> double
        if(sc.peek()=='.' && Character.isDigit(sc.peekNext())){
            d=true;
            sc.advance(); // preskacemo tacku
            while(Character.isDigit(sc.peek())){
                sc.advance();
            }
        }

        // od pocetka tokena do prve NE-cifre
        String text = source.substring(sc.getStartIdx(), sc.getCur());

        // ako je odmah posle broja cifra nevalidno
        char nextChar = sc.peek();
        if (Character.isAlphabetic(nextChar)) {
            throw error("Error: Character in int literal");
        }
        // dodajemo ga u listu tokena
        if(d) addLiteralDouble(text);
        else addLiteralInt(text);
    }

    // pravi identifikator
    private void identifier() {
        while (isIdentPart(sc.peek()))
            sc.advance();
        String text = source.substring(sc.getStartIdx(), sc.getCur());

        // ako ima kljuc u mapi s tim tekstom uzimamo njegov tip inace je Identifikator
        TokenType type = keywords.getOrDefault(text, TokenType.IDENT);

        if(type == TokenType.BOOL_LIT){
            addLiteralBool(text);
        }
        else{
            add(type, text);  // moramo da cuvamo ime promenljive
        }
    }

    private boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';   // dozvoljena slova i _ na pocetku
    }
    private boolean isIdentPart(char c)  {
        return isIdentStart(c) || Character.isDigit(c);  //provera za cifru tek od druge pozicije
    }

    private void charLit(){
        if(sc.isAtEnd()) throw error("Unterminated char literal");

        // trenutni karakter
        char c = sc.advance();

        // escape sekvence
        if (c == '\\') {
            char next = sc.advance();
            switch (next) {
                case 'n' -> c = '\n';
                case 't' -> c = '\t';
                case '\'' -> c = '\'';
                case '\\' -> c = '\\';
                default -> throw error("Invalid escape: \\" + next);
            }
        }

        // ako sledeci nije zatvoreni navodnik greska
        if (sc.peek() != '\'')
            throw error("Expected closing ' for char literal");

        sc.advance(); // preskacemo zatvoreni navodnik

        addLiteralChar(c);
    }


    private void stringLit(){
        StringBuilder sb = new StringBuilder();

        // dok ne dodjemo do kraja/zatvorenih navodnika krecemo se kroz string
        while (!sc.isAtEnd() && sc.peek() != '"') {
            char c = sc.advance();

            // escape sekvence
            if (c == '\\') {
                if (sc.isAtEnd()) throw error("Unterminated escape in string");

                char next = sc.advance();
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> throw error("Invalid escape: \\" + next);
                }
            } else {
                sb.append(c);
            }
        }

        // u slucaju da nismo dosli do zatvorenih " a dosli smo do kraja
        if (sc.isAtEnd()) throw error("Unterminated string literal");

        sc.advance(); // preskacemo navodnike za zatvaranje

        addLiteralString(sb.toString());
    }

    // za simbole
    private void add(TokenType type) {
        String lex = source.substring(sc.getStartIdx(), sc.getCur());
        tokeni.add(new Token(type, lex, null,
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    // za identifikatore
    private void add(TokenType type, String text) {
        tokeni.add(new Token(type, text, null,
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void addLiteralInt(String literal) {
        tokeni.add(new Token(TokenType.INT_LIT, literal, Integer.valueOf(literal),
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void addLiteralDouble(String literal) {
        tokeni.add(new Token(TokenType.DOUBLE_LIT, literal, Double.valueOf(literal),
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void addLiteralBool(String literal) {
        tokeni.add(new Token(TokenType.BOOL_LIT, literal, Boolean.valueOf(literal),
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void addLiteralChar(char literal) {
        tokeni.add(new Token(TokenType.CHAR_LIT, String.valueOf(literal), literal,
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void addLiteralString(String literal) {
        tokeni.add(new Token(TokenType.STRING_LIT, literal, literal,
                sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private RuntimeException error(String msg) {
        String near = source.substring(sc.getStartIdx(), Math.min(sc.getCur(), source.length()));
        return new RuntimeException("LEXER > " + msg + " at " + sc.getStartLine() + ":" + sc.getStartCol() + " near '" + near + "'");
    }
}
