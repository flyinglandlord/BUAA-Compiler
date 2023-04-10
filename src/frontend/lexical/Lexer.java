package frontend.lexical;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final String rawCode;
    private final List<Token> tokenList = new ArrayList<>();

    public Lexer(String rawCode) {
        this.rawCode = rawCode;
        generateTokenList();
    }

    public String getRawCode() {
        return rawCode;
    }

    public List<Token> getTokenList() {
        return tokenList;
    }

    private int parseNumber(StringBuilder sb, int index) {
        int i = index;
        while (i < rawCode.length() && Character.isDigit(rawCode.charAt(i))) {
            sb.append(rawCode.charAt(i));
            i++;
        }
        return i;
    }

    private int parseIdentifier(StringBuilder sb, int index) {
        int i = index;
        while (i < rawCode.length() &&
                (Character.isLetter(rawCode.charAt(i)) ||
                Character.isDigit(rawCode.charAt(i)) ||
                rawCode.charAt(i) == '_')) {
            sb.append(rawCode.charAt(i));
            i++;
        }
        return i;
    }

    private void generateTokenList() {
        int linenumber = 1;
        int i = 0;
        while (i < rawCode.length()) {
            //System.out.print(i);
            //System.out.print(rawCode.charAt(i));
            while (i < rawCode.length() &&
                    (rawCode.charAt(i) == ' ' || rawCode.charAt(i) == '\n' ||
                     rawCode.charAt(i) == '\t' || rawCode.charAt(i) == '\r')) {
                if (rawCode.charAt(i) == '\n') linenumber++;
                i++;
            }
            if (i == rawCode.length()) break;
            if (Character.isDigit(rawCode.charAt(i))) {
                StringBuilder numberBuilder = new StringBuilder();
                i = parseNumber(numberBuilder, i);
                tokenList.add(new Token(numberBuilder.toString(), Token.TokenType.INTCON,
                        linenumber, Integer.parseInt(numberBuilder.toString())));
            } else if (Character.isLetter(rawCode.charAt(i)) || rawCode.charAt(i) == '_') {
                StringBuilder wordBuilder = new StringBuilder();
                i = parseIdentifier(wordBuilder, i);
                tokenList.add(new Token(wordBuilder.toString(),
                        Token.tokenMap.getOrDefault(wordBuilder.toString(), Token.TokenType.IDENFR),
                        linenumber, -1));
            } else if ("+-*%;,()[]{}".indexOf(rawCode.charAt(i)) != -1) {
                tokenList.add(new Token(String.valueOf(rawCode.charAt(i)),
                        Token.tokenMap.get(String.valueOf(rawCode.charAt(i))), linenumber, -1));
                i++;
            } else if (rawCode.charAt(i) == '/') {
                if (i+1 < rawCode.length() && rawCode.charAt(i+1) == '/') {
                    while (rawCode.charAt(i) != '\n') i++;
                } else if (i+1 < rawCode.length() && rawCode.charAt(i+1) == '*') {
                    i += 2;
                    while (i+1 < rawCode.length() &&
                            !(rawCode.charAt(i) == '*' && rawCode.charAt(i+1) == '/')) {
                        if (rawCode.charAt(i) == '\n') linenumber++;
                        i++;
                    }
                    i += 2;
                } else {
                    tokenList.add(new Token(String.valueOf(rawCode.charAt(i)),
                            Token.TokenType.DIV, linenumber, -1));
                    i++;
                }
            } else if (rawCode.charAt(i) == '&') {
                if (i+1 < rawCode.length() && rawCode.charAt(i+1) == '&') {
                    tokenList.add(new Token("&&", Token.TokenType.AND, linenumber, -1));
                    i += 2;
                } else {
                    tokenList.add(new Token("&", Token.TokenType.ERR, linenumber, -1));
                    i++;
                }
            } else if (rawCode.charAt(i) == '|') {
                if (i+1 < rawCode.length() && rawCode.charAt(i+1) == '|') {
                    tokenList.add(new Token("||", Token.TokenType.OR, linenumber, -1));
                    i += 2;
                } else {
                    tokenList.add(new Token("|", Token.TokenType.ERR, linenumber, -1));
                    i++;
                }
            } else if ("<>=!".indexOf(rawCode.charAt(i)) != -1) {
                if (i+1 < rawCode.length() && rawCode.charAt(i+1) == '=') {
                    tokenList.add(new Token(rawCode.charAt(i) + "=",
                            Token.tokenMap.get(rawCode.charAt(i) + "="), linenumber, -1));
                    i += 2;
                } else {
                    tokenList.add(new Token(String.valueOf(rawCode.charAt(i)),
                            Token.tokenMap.get(String.valueOf(rawCode.charAt(i))), linenumber, -1));
                    i++;
                }
            } else if (rawCode.charAt(i) == '\"') {
                StringBuilder charBuilder = new StringBuilder();
                i++;
                charBuilder.append("\"");
                while (i < rawCode.length() && rawCode.charAt(i) != '\"') {
                    charBuilder.append(rawCode.charAt(i));
                    i++;
                }
                charBuilder.append("\"");
                i++;
                tokenList.add(new Token(charBuilder.toString(), Token.TokenType.STRCON, linenumber, -1));
            } else {
                tokenList.add(new Token(String.valueOf(rawCode.charAt(i)),
                        Token.TokenType.ERR, linenumber, -1));
            }
        }
    }
}
