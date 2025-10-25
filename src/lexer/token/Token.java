package lexer.token;

public class Token {
    public final TokenType type;
    public final String lexeme;
    // jer imamo vise tipova literala
    public final Object literal;

    public final int line, colStart, colEnd;

    public Token(TokenType type, String lexeme, Object literal, int line, int colStart, int colEnd) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.colStart = colStart;
        this.colEnd = colEnd;
    }

    public String toString(){
        return (type+" '"+lexeme+"' at line: "+line+", column: "+colStart).
                replace("\n", "\\n").
                replace("\0", "\\0");
    }


}
