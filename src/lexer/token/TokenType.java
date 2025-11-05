package lexer.token;

public enum TokenType {
    // tipovi podataka
    XONUM, XODOUBLE, XOCHAR, XOBOOL, XOSTRING, XOARRAY, XOVOID,

    // literali
    INT_LIT, DOUBLE_LIT, STRING_LIT, CHAR_LIT, BOOL_LIT,

    // ime promenljive
    IDENT,

    // osnovne funkcije
    XOTWOD, EXODUS, CALLOUT, LISTEN, ECHO,

    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, COMMA, SEMICOLON,

    // operatori
    ASSIGN,
    ADD, SUBTRACT, MULTIPLY, DIVIDE, PERCENT,
    RISE, FADE,
    EQ, NEQ, LT, GT, LE, GE,
    XOAND, XOOR, XONOT,

    TRYME, NEXT, ORNAH,
    REPEATAFTERME, TIMELESS,
    EOF
}
