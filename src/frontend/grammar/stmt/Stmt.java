package frontend.grammar.stmt;

import frontend.grammar.Node;
import frontend.lexical.Token;

import java.io.PrintStream;

public abstract class Stmt implements BlockItem {
    private final Token semicolon;
    private final int linenumber;

    public Stmt(Token semicolon, int linenumber) {
        this.semicolon = semicolon;
        this.linenumber = linenumber;
    }

    public Token getSemicolon() {
        return semicolon;
    }

    @Override
    public void print(PrintStream out) {
        if (semicolon != null) {
            out.println(semicolon);
        }
        out.println("<Stmt>");
    }
}
