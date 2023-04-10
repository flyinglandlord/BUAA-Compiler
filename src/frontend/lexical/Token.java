package frontend.lexical;

import java.util.HashMap;
import java.util.Map;

public class Token {
    public enum TokenType {
        IDENFR, INTCON, STRCON, MAINTK, CONSTTK,
        INTTK, BREAKTK, CONTINUETK, IFTK, ELSETK,
        NOT, AND, OR, WHILETK, GETINTTK, PRINTFTK,
        RETURNTK, PLUS, MINU, VOIDTK, MULT, DIV,
        MOD, LSS, LEQ, GRE, GEQ, EQL, NEQ, ASSIGN,
        SEMICN, COMMA, LPARENT, RPARENT, LBRACK,
        RBRACK, LBRACE, RBRACE, ERR,
        BITAND
    }

    public static final Map<String, TokenType> tokenMap = new HashMap<>();
    static {
        tokenMap.put("main", TokenType.MAINTK);
        tokenMap.put("const", TokenType.CONSTTK);
        tokenMap.put("int", TokenType.INTTK);
        tokenMap.put("break", TokenType.BREAKTK);
        tokenMap.put("continue", TokenType.CONTINUETK);
        tokenMap.put("if", TokenType.IFTK);
        tokenMap.put("else", TokenType.ELSETK);
        tokenMap.put("!", TokenType.NOT);
        tokenMap.put("&&", TokenType.AND);
        tokenMap.put("||", TokenType.OR);
        tokenMap.put("while", TokenType.WHILETK);
        tokenMap.put("getint", TokenType.GETINTTK);
        tokenMap.put("printf", TokenType.PRINTFTK);
        tokenMap.put("return", TokenType.RETURNTK);
        tokenMap.put("+", TokenType.PLUS);
        tokenMap.put("-", TokenType.MINU);
        tokenMap.put("void", TokenType.VOIDTK);
        tokenMap.put("*", TokenType.MULT);
        tokenMap.put("/", TokenType.DIV);
        tokenMap.put("%", TokenType.MOD);
        tokenMap.put("<", TokenType.LSS);
        tokenMap.put("<=", TokenType.LEQ);
        tokenMap.put(">", TokenType.GRE);
        tokenMap.put(">=", TokenType.GEQ);
        tokenMap.put("==", TokenType.EQL);
        tokenMap.put("!=", TokenType.NEQ);
        tokenMap.put("=", TokenType.ASSIGN);
        tokenMap.put(";", TokenType.SEMICN);
        tokenMap.put(",", TokenType.COMMA);
        tokenMap.put("(", TokenType.LPARENT);
        tokenMap.put(")", TokenType.RPARENT);
        tokenMap.put("[", TokenType.LBRACK);
        tokenMap.put("]", TokenType.RBRACK);
        tokenMap.put("{", TokenType.LBRACE);
        tokenMap.put("}", TokenType.RBRACE);

        tokenMap.put("bitand", TokenType.BITAND);
    }

    private final String content;
    private final int value;
    private final TokenType type;
    private final int linenumber;

    public Token(String content, TokenType type, int linenumber, int value) {
        this.content = content;
        this.type = type;
        this.linenumber = linenumber;
        this.value = value;
    }

    public TokenType getTokenType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getLinenumber() {
        return linenumber;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return type.toString() + " " + content;
    }
}
