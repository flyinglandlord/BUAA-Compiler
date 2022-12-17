package frontend.grammar.stmt;

import frontend.lexical.Token;

import java.io.PrintStream;

// 'continue' ';'
public class ContinueStmt extends Stmt {
    private final Token continueToken;

    public ContinueStmt(Token continueToken, Token semicolon) {
        super(semicolon, continueToken.getLinenumber());
        this.continueToken = continueToken;
    }

    public Token getContinueToken() {
        return continueToken;
    }

    @Override
    public void print(PrintStream out) {
        out.println(continueToken.toString());
        super.print(out);
    }
}
