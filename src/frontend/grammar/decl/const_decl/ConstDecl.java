package frontend.grammar.decl.const_decl;

import frontend.grammar.decl.Decl;
import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.List;

public class ConstDecl extends Decl {
    private final Token constToken;
    private final Token BType;
    private final ConstDef first;
    private final List<ConstDef> rest;
    private final List<Token> commas;
    private final Token semicolon;

    public ConstDecl(Token constToken, Token bType, ConstDef first,
                     List<ConstDef> rest, List<Token> commas, Token semicolon) {
        this.constToken = constToken;
        this.BType = bType;
        this.rest = rest;
        this.first = first;
        this.commas = commas;
        this.semicolon = semicolon;
    }

    public Token getConstToken() {
        return constToken;
    }

    public Token getBType() {
        return BType;
    }

    public ConstDef getFirst() {
        return first;
    }

    public List<ConstDef> getRest() {
        return rest;
    }

    public List<Token> getCommas() {
        return commas;
    }

    public Token getSemicolon() {
        return semicolon;
    }

    @Override
    public void print(PrintStream out) {
        out.println(constToken.toString());
        out.println(BType.toString());
        first.print(out);
        if (commas != null) {
            for (int i = 0; i < commas.size(); i++) {
                out.println(commas.get(i).toString());
                rest.get(i).print(out);
            }
        }
        if (semicolon != null) {
            out.println(semicolon.toString());
        }
        out.println("<ConstDecl>");
    }
}
