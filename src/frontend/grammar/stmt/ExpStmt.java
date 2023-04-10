package frontend.grammar.stmt;

import frontend.grammar.expr.linked_expr.Exp;
import frontend.lexical.Token;

import java.io.PrintStream;

//  [Exp] ';'
public class ExpStmt extends Stmt {
    private final Exp exp;

    public ExpStmt(Exp exp, Token semicolon) {
        super(semicolon, exp.getLinenumber());
        this.exp = exp;
    }

    public Exp getExp() {
        return exp;
    }

    @Override
    public void print(PrintStream out) {
        exp.print(out);
        super.print(out);
    }
}
