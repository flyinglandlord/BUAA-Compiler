package frontend.grammar.stmt;

import frontend.lexical.Token;

import java.io.PrintStream;

// 'break' ';'
public class BreakStmt extends Stmt {
    private final Token breakToken;

    public BreakStmt(Token breakToken, Token semicolon) {
        super(semicolon, breakToken.getLinenumber());
        this.breakToken = breakToken;
    }

    public Token getBreakToken() {
        return breakToken;
    }

    @Override
    public void print(PrintStream out) {
        out.println(breakToken.toString());
        super.print(out);
    }
}
