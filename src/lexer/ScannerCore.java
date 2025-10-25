package lexer;

// klasa za citanje karaktera i pracenje pozicije
public class ScannerCore {

    // sadrzaj fajla kao string
    private final String src;

    // globalni kursor
    private int cur = 0;
    private int line = 1;
    private int col = 1;

    // kursori za kretanje
    private int startIdx = 0;
    private int startLine = 1;
    private int startCol = 1;

    public ScannerCore(String src) {
        this.src = src;
    }

    public boolean isAtEnd() {
        return cur >= src.length();
    }

    // vraca trenutni karakter (bez pomeranja kursora)
    public char peek() {
        return isAtEnd() ? '\0' : src.charAt(cur);
    }

    // vraca sledeci karakter (bez pomeranja kursora)
    public char peekNext() {
        return (cur + 1 >= src.length()) ? '\0' : src.charAt(cur + 1);
    }

    // uzima trenutni i pomera se na sledeci (pamti odgovarajuce pozicije)
    public char advance() {
        char c = src.charAt(cur++);
        if (c == '\n') {
            line++; col = 1;
        } else {
            col++;
        }
        return c;
    }

    public boolean match(char expected) {
        if (isAtEnd() || src.charAt(cur) != expected) return false;
        cur++; col++;
        return true;
    }

    // startujemo token postavljanjem odgovarajucih pozicija
    public void beginToken() {
        startIdx = cur;
        startLine = line;
        startCol = col;
    }

    public int getCur() { return cur; }
    public int getLine() { return line; }
    public int getCol() { return col; }
    public int getStartIdx() { return startIdx; }
    public int getStartLine() { return startLine; }
    public int getStartCol() { return startCol; }

}
