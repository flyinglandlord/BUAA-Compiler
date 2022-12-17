package frontend.grammar.decl.var_decl;

import frontend.grammar.decl.Decl;
import frontend.lexical.Token;

import java.io.PrintStream;
import java.util.List;

public class VarDecl extends Decl {
    private final Token BType;
    private final VarDef first;
    private final List<VarDef> rest;
    private final List<Token> commas;
    private final Token semicolon;

    public VarDecl(Token bType, VarDef first,
                   List<VarDef> rest, List<Token> commas, Token semicolon) {
        this.BType = bType;
        this.rest = rest;
        this.first = first;
        this.commas = commas;
        this.semicolon = semicolon;
    }

    public Token getBType() {
        return BType;
    }

    public VarDef getFirst() {
        return first;
    }

    public List<VarDef> getRest() {
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
        out.println("<VarDecl>");
    }
}
