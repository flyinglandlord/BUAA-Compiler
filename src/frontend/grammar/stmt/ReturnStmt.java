package frontend.grammar.stmt;

import frontend.grammar.expr.linked_expr.Exp;
import frontend.lexical.Token;

import java.io.PrintStream;

// 'return' [Exp] ';'
public class ReturnStmt extends Stmt {
    private final Token returnToken;
    private final Exp exp;

    public ReturnStmt(Token returnToken, Exp exp, Token semicolon) {
        super(semicolon, returnToken.getLinenumber());
        this.returnToken = returnToken;
        this.exp = exp;
    }

    public ReturnStmt(Token returnToken, Token semicolon) {
        super(semicolon, returnToken.getLinenumber());
        this.returnToken = returnToken;
        this.exp = null;
    }

    public Token getReturnToken() {
        return returnToken;
    }

    public Exp getExp() {
        return exp;
    }

    @Override
    public void print(PrintStream out) {
        out.println(returnToken.toString());
        if (exp != null) {
            exp.print(out);
        }
        super.print(out);
    }
}
